package com.ragplatform.application.fakes;

import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.DocumentStatus;
import com.ragplatform.domain.model.SourceDocument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryDocumentRepository implements DocumentRepositoryPort {

    public final Map<DocumentId, SourceDocument> store = new LinkedHashMap<>();
    public final List<DocumentStatus> statusHistory = new ArrayList<>();

    @Override
    public void save(SourceDocument document) {
        store.put(document.id(), document);
        statusHistory.add(document.status());
    }

    @Override
    public Optional<SourceDocument> findById(DocumentId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<SourceDocument> findByCollectionAndSha256(String collection, String sha256) {
        return store.values().stream()
                .filter(d -> d.collection().equals(collection) && d.sha256().equals(sha256)).findFirst();
    }

    @Override
    public List<SourceDocument> findByCollection(String collection) {
        return store.values().stream().filter(d -> d.collection().equals(collection)).toList();
    }
}
