package com.ragplatform.application.service;

import com.ragplatform.application.port.in.QueryDocumentsUseCase;
import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.domain.exception.DocumentNotFoundException;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.SourceDocument;
import java.util.List;

public final class QueryDocumentsService implements QueryDocumentsUseCase {

    private final DocumentRepositoryPort repository;
    private final CollectionPolicy collections;

    public QueryDocumentsService(DocumentRepositoryPort repository, CollectionPolicy collections) {
        this.repository = repository;
        this.collections = collections;
    }

    @Override
    public SourceDocument get(DocumentId id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Override
    public List<SourceDocument> listByCollection(String collection) {
        collections.requireKnown(collection);
        return repository.findByCollection(collection);
    }
}
