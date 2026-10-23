package com.ragplatform.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identity of a source document. Value object: immutable, compared by value. */
public record DocumentId(UUID value) {

    public DocumentId {
        Objects.requireNonNull(value, "value");
    }

    public static DocumentId newId() {
        return new DocumentId(UUID.randomUUID());
    }

    public static DocumentId of(String raw) {
        return new DocumentId(UUID.fromString(raw.trim()));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
