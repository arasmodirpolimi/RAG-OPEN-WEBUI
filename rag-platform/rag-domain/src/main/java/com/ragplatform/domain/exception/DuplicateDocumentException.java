package com.ragplatform.domain.exception;

import com.ragplatform.domain.model.DocumentId;

public class DuplicateDocumentException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final transient DocumentId existingId;

    public DuplicateDocumentException(DocumentId existingId) {
        super("Identical file already uploaded to this collection: " + existingId);
        this.existingId = existingId;
    }

    public DocumentId existingId() {
        return existingId;
    }
}
