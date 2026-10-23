package com.ragplatform.application.port.in;

import com.ragplatform.domain.model.DocumentId;

public interface ReindexDocumentUseCase {

    void reindex(DocumentId documentId);
}
