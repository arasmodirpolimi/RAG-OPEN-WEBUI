package com.ragplatform.domain.model;

import com.ragplatform.domain.exception.InvalidStateTransitionException;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root for an uploaded document. State changes return new instances,
 * and every transition is validated here, so no other layer can put a document
 * into an inconsistent state.
 */
public record SourceDocument(
        DocumentId id,
        String collection,
        String fileName,
        String mimeType,
        String sha256,
        String storageKey,
        DocumentStatus status,
        String errorMessage,
        Instant createdAt,
        Instant indexedAt) {

    public SourceDocument {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(storageKey, "storageKey");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static SourceDocument newlyUploaded(DocumentId id, String collection, String fileName,
                                               String mimeType, String sha256, String storageKey,
                                               Instant now) {
        return new SourceDocument(id, collection, fileName, mimeType, sha256, storageKey,
                DocumentStatus.PENDING, null, now, null);
    }

    public SourceDocument markProcessing() {
        if (status == DocumentStatus.INDEXED) {
            // Re-processing an indexed document must go through markPending (explicit reindex).
            throw new InvalidStateTransitionException(id, status, DocumentStatus.PROCESSING);
        }
        return withStatus(DocumentStatus.PROCESSING, null, indexedAt);
    }

    public SourceDocument markIndexed(Instant when) {
        if (status != DocumentStatus.PROCESSING) {
            throw new InvalidStateTransitionException(id, status, DocumentStatus.INDEXED);
        }
        return withStatus(DocumentStatus.INDEXED, null, when);
    }

    public SourceDocument markFailed(String reason) {
        return withStatus(DocumentStatus.FAILED, reason, indexedAt);
    }

    public SourceDocument markPending() {
        if (status == DocumentStatus.PROCESSING) {
            throw new InvalidStateTransitionException(id, status, DocumentStatus.PENDING);
        }
        return withStatus(DocumentStatus.PENDING, null, indexedAt);
    }

    private SourceDocument withStatus(DocumentStatus newStatus, String error, Instant indexed) {
        return new SourceDocument(id, collection, fileName, mimeType, sha256, storageKey,
                newStatus, error, createdAt, indexed);
    }
}
