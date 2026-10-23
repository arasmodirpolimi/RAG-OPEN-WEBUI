package com.ragplatform.application.port.in;

import com.ragplatform.domain.model.DocumentId;

/** Parse, chunk, embed and index one document. Throws on failure so the caller can retry. */
public interface ProcessDocumentUseCase {

    void process(DocumentId documentId);
}
