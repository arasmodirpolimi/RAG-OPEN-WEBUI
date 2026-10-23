package com.ragplatform.domain.model;

/** One page of parser output, as Markdown. Word files have no fixed pages and arrive as page 1. */
public record ParsedPage(int number, String markdown) {

    public ParsedPage {
        if (number < 1) {
            throw new IllegalArgumentException("page number must be >= 1");
        }
        markdown = markdown == null ? "" : markdown;
    }
}
