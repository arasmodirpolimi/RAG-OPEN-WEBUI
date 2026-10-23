package com.ragplatform.domain.model;

/** A numbered source shown to the user; the number matches the [n] markers in the answer. */
public record Citation(int number, String fileName, int pageStart, int pageEnd, String sectionPath) {

    public static Citation of(int number, Chunk chunk) {
        return new Citation(number, chunk.fileName(), chunk.pageStart(), chunk.pageEnd(), chunk.sectionPath());
    }

    public String pageLabel() {
        return pageStart == pageEnd ? "p. " + pageStart : "pp. " + pageStart + "-" + pageEnd;
    }
}
