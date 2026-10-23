package com.ragplatform.domain.exception;

import com.ragplatform.domain.model.DocumentId;

public class DocumentNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public DocumentNotFoundException(DocumentId id) {
        super("Document not found: " + id);
    }
}
