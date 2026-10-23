package com.ragplatform.api.web.documents;

import com.ragplatform.domain.model.SourceDocument;
import java.time.Instant;

public record DocumentView(String id, String collection, String fileName, String status,
                           String errorMessage, Instant createdAt, Instant indexedAt) {

    static DocumentView of(SourceDocument d) {
        return new DocumentView(d.id().toString(), d.collection(), d.fileName(), d.status().name(),
                d.errorMessage(), d.createdAt(), d.indexedAt());
    }
}
