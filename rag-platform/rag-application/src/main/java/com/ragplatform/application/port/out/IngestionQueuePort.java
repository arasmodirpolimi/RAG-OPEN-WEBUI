package com.ragplatform.application.port.out;

import com.ragplatform.domain.model.DocumentId;

public interface IngestionQueuePort {

    void enqueue(DocumentId documentId);
}
