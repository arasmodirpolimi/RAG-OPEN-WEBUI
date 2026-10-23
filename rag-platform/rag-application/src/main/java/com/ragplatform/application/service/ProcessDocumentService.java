package com.ragplatform.application.service;

import com.ragplatform.agent.chunking.DocumentChunker;
import com.ragplatform.agent.port.EmbeddingPort;
import com.ragplatform.application.port.in.ProcessDocumentUseCase;
import com.ragplatform.application.port.out.ChunkIndexPort;
import com.ragplatform.application.port.out.DocumentParserPort;
import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.application.port.out.FileStoragePort;
import com.ragplatform.domain.exception.DocumentNotFoundException;
import com.ragplatform.domain.exception.DomainException;
import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.DocumentStatus;
import com.ragplatform.domain.model.EmbeddedChunk;
import com.ragplatform.domain.model.ParsedDocument;
import com.ragplatform.domain.model.SourceDocument;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Orchestrates ingestion: load -> parse -> chunk -> embed -> index, tracking status throughout. */
public final class ProcessDocumentService implements ProcessDocumentUseCase {

    private final DocumentRepositoryPort repository;
    private final FileStoragePort storage;
    private final DocumentParserPort parser;
    private final DocumentChunker chunker;
    private final EmbeddingPort embeddings;
    private final ChunkIndexPort index;
    private final Clock clock;
    private final int embeddingBatchSize;

    public ProcessDocumentService(DocumentRepositoryPort repository, FileStoragePort storage,
                                  DocumentParserPort parser, DocumentChunker chunker,
                                  EmbeddingPort embeddings, ChunkIndexPort index, Clock clock,
                                  int embeddingBatchSize) {
        if (embeddingBatchSize < 1) {
            throw new IllegalArgumentException("embeddingBatchSize must be >= 1");
        }
        this.repository = repository;
        this.storage = storage;
        this.parser = parser;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.index = index;
        this.clock = clock;
        this.embeddingBatchSize = embeddingBatchSize;
    }

    @Override
    public void process(DocumentId documentId) {
        SourceDocument document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (document.status() == DocumentStatus.INDEXED) {
            return; // duplicate delivery of an already processed message
        }
        SourceDocument processing = document.markProcessing();
        repository.save(processing);
        try {
            byte[] content = storage.load(processing.storageKey());
            ParsedDocument parsed = parser.parse(processing.fileName(), processing.mimeType(), content);
            if (parsed.isEmpty()) {
                throw new DomainException("No text could be extracted from " + processing.fileName());
            }
            List<Chunk> chunks = chunker.chunk(processing, parsed);
            index.replaceDocumentChunks(documentId, embed(chunks));
            repository.save(processing.markIndexed(clock.instant()));
        } catch (RuntimeException e) {
            repository.save(processing.markFailed(rootMessage(e)));
            throw e;
        }
    }

    private List<EmbeddedChunk> embed(List<Chunk> chunks) {
        List<EmbeddedChunk> result = new ArrayList<>(chunks.size());
        for (int from = 0; from < chunks.size(); from += embeddingBatchSize) {
            List<Chunk> batch = chunks.subList(from, Math.min(chunks.size(), from + embeddingBatchSize));
            List<float[]> vectors = embeddings.embed(batch.stream().map(Chunk::contextualText).toList());
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("Embedding count mismatch: " + vectors.size() + " for " + batch.size());
            }
            for (int i = 0; i < batch.size(); i++) {
                result.add(new EmbeddedChunk(batch.get(i), vectors.get(i)));
            }
        }
        return result;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }
}
