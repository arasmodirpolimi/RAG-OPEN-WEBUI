package com.ragplatform.application.service;

import com.ragplatform.application.port.in.ReindexDocumentUseCase;
import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.application.port.out.IngestionQueuePort;
import com.ragplatform.domain.exception.DocumentNotFoundException;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.SourceDocument;

public final class ReindexDocumentService implements ReindexDocumentUseCase {

    private final DocumentRepositoryPort repository;
    private final IngestionQueuePort queue;

    public ReindexDocumentService(DocumentRepositoryPort repository, IngestionQueuePort queue) {
        this.repository = repository;
        this.queue = queue;
    }

    @Override
    public void reindex(DocumentId documentId) {
        SourceDocument document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        repository.save(document.markPending());
        queue.enqueue(documentId);
    }
}
