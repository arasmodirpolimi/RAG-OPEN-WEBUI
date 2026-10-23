package com.ragplatform.domain.model;

/** Lifecycle of a document inside the ingestion pipeline. */
public enum DocumentStatus {
    PENDING,
    PROCESSING,
    INDEXED,
    FAILED
}
