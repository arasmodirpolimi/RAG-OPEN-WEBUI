package com.ragplatform.application.port.out;

import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.SourceDocument;
import java.util.List;
import java.util.Optional;

public interface DocumentRepositoryPort {

    /** Insert or update by id. */
    void save(SourceDocument document);

    Optional<SourceDocument> findById(DocumentId id);

    Optional<SourceDocument> findByCollectionAndSha256(String collection, String sha256);

    List<SourceDocument> findByCollection(String collection);
}
