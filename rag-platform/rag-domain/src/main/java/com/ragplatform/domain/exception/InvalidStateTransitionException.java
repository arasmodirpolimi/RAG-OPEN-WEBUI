package com.ragplatform.domain.exception;

import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.DocumentStatus;

public class InvalidStateTransitionException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(DocumentId id, DocumentStatus from, DocumentStatus to) {
        super("Document " + id + " cannot move from " + from + " to " + to);
    }
}
