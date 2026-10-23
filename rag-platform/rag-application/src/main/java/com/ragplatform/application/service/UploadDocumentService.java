package com.ragplatform.application.service;

import com.ragplatform.application.port.in.UploadDocumentUseCase;
import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.application.port.out.FileStoragePort;
import com.ragplatform.application.port.out.IngestionQueuePort;
import com.ragplatform.domain.exception.DuplicateDocumentException;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.SourceDocument;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

public final class UploadDocumentService implements UploadDocumentUseCase {

    private final DocumentRepositoryPort repository;
    private final FileStoragePort storage;
    private final IngestionQueuePort queue;
    private final CollectionPolicy collections;
    private final Clock clock;

    public UploadDocumentService(DocumentRepositoryPort repository, FileStoragePort storage,
                                 IngestionQueuePort queue, CollectionPolicy collections, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.queue = queue;
        this.collections = collections;
        this.clock = clock;
    }

    @Override
    public DocumentId upload(UploadDocumentCommand command) {
        collections.requireKnown(command.collection());
        String sha256 = sha256(command.content());
        repository.findByCollectionAndSha256(command.collection(), sha256).ifPresent(existing -> {
            throw new DuplicateDocumentException(existing.id());
        });

        DocumentId id = DocumentId.newId();
        String key = command.collection() + "/" + id + "/" + safeFileName(command.fileName());
        storage.store(key, command.content());
        repository.save(SourceDocument.newlyUploaded(id, command.collection(), command.fileName(),
                command.mimeType(), sha256, key, clock.instant()));
        // If enqueueing fails the document stays PENDING and can be re-queued via reindex.
        queue.enqueue(id);
        return id;
    }

    static String safeFileName(String fileName) {
        String base = fileName.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() || cleaned.startsWith(".") ? "file" + cleaned : cleaned;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
