package com.ragplatform.application.port.out;

import com.ragplatform.domain.model.ParsedDocument;

/** Converts a binary file (PDF, Word, image...) into structured Markdown pages, with OCR where needed. */
public interface DocumentParserPort {

    ParsedDocument parse(String fileName, String mimeType, byte[] content);
}
