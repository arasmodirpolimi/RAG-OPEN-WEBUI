package com.ragplatform.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ragplatform.agent.chunking.ChunkingSettings;
import com.ragplatform.agent.chunking.MarkdownStructureChunker;
import com.ragplatform.agent.chunking.RepeatedLineFilter;
import com.ragplatform.agent.chunking.WordBasedTokenEstimator;
import com.ragplatform.application.fakes.InMemoryDocumentRepository;
import com.ragplatform.application.fakes.InMemoryFileStorage;
import com.ragplatform.application.port.out.DocumentParserPort;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.DocumentStatus;
import com.ragplatform.domain.model.EmbeddedChunk;
import com.ragplatform.domain.model.ParsedDocument;
import com.ragplatform.domain.model.ParsedPage;
import com.ragplatform.domain.model.SourceDocument;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessDocumentServiceTest {

    private final InMemoryDocumentRepository repo = new InMemoryDocumentRepository();
    private final InMemoryFileStorage storage = new InMemoryFileStorage();
    private final List<EmbeddedChunk> indexed = new ArrayList<>();
    private final SourceDocument doc = SourceDocument.newlyUploaded(DocumentId.newId(), "contratti",
            "c.pdf", "application/pdf", "sha", "contratti/c.pdf", Instant.EPOCH);

    private ProcessDocumentService service(DocumentParserPort parser) {
        return new ProcessDocumentService(repo, storage, parser,
                new MarkdownStructureChunker(new ChunkingSettings(400, 50), new WordBasedTokenEstimator(),
                        new RepeatedLineFilter()),
                texts -> texts.stream().map(t -> new float[] {0.1f, 0.2f}).toList(),
                (id, chunks) -> indexed.addAll(chunks),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), 2);
    }

    @Test
    void indexesChunksAndMarksIndexed() {
        repo.save(doc);
        storage.store(doc.storageKey(), new byte[] {1});
        DocumentParserPort parser = (name, mime, bytes) -> new ParsedDocument(List.of(
                new ParsedPage(1, "# A\n\nuno"), new ParsedPage(2, "# B\n\ndue"), new ParsedPage(3, "# C\n\ntre")));

        service(parser).process(doc.id());

        assertEquals(3, indexed.size(), "batching (size 2) must not lose chunks");
        assertEquals(List.of(DocumentStatus.PENDING, DocumentStatus.PROCESSING, DocumentStatus.INDEXED),
                repo.statusHistory);
    }

    @Test
    void parserFailureMarksFailedAndRethrows() {
        repo.save(doc);
        storage.store(doc.storageKey(), new byte[] {1});
        DocumentParserPort failing = (name, mime, bytes) -> {
            throw new IllegalStateException("docling unavailable");
        };

        assertThrows(IllegalStateException.class, () -> service(failing).process(doc.id()));

        SourceDocument after = repo.store.get(doc.id());
        assertEquals(DocumentStatus.FAILED, after.status());
        assertEquals("docling unavailable", after.errorMessage());
    }

    @Test
    void emptyExtractionFails() {
        repo.save(doc);
        storage.store(doc.storageKey(), new byte[] {1});
        assertThrows(RuntimeException.class, () -> service((n, m, b) ->
                new ParsedDocument(List.of(new ParsedPage(1, "  ")))).process(doc.id()));
        assertEquals(DocumentStatus.FAILED, repo.store.get(doc.id()).status());
    }
}
