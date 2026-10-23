package com.ragplatform.domain.model;

import java.util.List;

/** Structured parser output: the document's pages in reading order. */
public record ParsedDocument(List<ParsedPage> pages) {

    public ParsedDocument {
        pages = List.copyOf(pages);
    }

    public boolean isEmpty() {
        return pages.stream().allMatch(p -> p.markdown().isBlank());
    }
}
