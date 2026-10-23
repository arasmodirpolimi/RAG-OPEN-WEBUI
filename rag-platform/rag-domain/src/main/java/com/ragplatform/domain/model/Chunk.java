package com.ragplatform.domain.model;

import java.util.Objects;

/** A retrievable unit of a document, with everything needed to cite it. */
public record Chunk(
        String id,
        DocumentId documentId,
        String collection,
        String fileName,
        int pageStart,
        int pageEnd,
        String sectionPath,
        String text,
        ChunkType type) {

    public Chunk {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(text, "text");
        sectionPath = sectionPath == null ? "" : sectionPath;
        if (pageEnd < pageStart) {
            throw new IllegalArgumentException("pageEnd < pageStart");
        }
    }

    /** Deterministic id, so re-indexing the same document overwrites instead of duplicating. */
    public static String idFor(DocumentId documentId, int ordinal) {
        return documentId + "-" + ordinal;
    }

    /** Text prefixed with its section path; used for embeddings and reranking. */
    public String contextualText() {
        return sectionPath.isBlank() ? text : sectionPath + "\n\n" + text;
    }
}
