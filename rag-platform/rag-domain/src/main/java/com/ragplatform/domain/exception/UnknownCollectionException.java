package com.ragplatform.domain.exception;

public class UnknownCollectionException extends DomainException {

    private static final long serialVersionUID = 1L;

    public UnknownCollectionException(String collection) {
        super("Unknown collection: " + collection);
    }
}
