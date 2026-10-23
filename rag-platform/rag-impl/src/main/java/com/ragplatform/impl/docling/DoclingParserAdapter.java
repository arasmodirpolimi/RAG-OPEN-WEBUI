package com.ragplatform.impl.docling;

import com.fasterxml.jackson.databind.JsonNode;
import com.ragplatform.application.port.out.DocumentParserPort;
import com.ragplatform.domain.model.ParsedDocument;
import com.ragplatform.domain.model.ParsedPage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Docling Serve POST /v1/convert/file (multipart). Requests Markdown with a page-break placeholder
 * (md_page_break_placeholder) so page numbers survive for citations. OCR is applied to bitmap content.
 */
public final class DoclingParserAdapter implements DocumentParserPort {

    static final String PAGE_BREAK = "<!-- rag-page-break -->";

    private final RestClient http;
    private final List<String> ocrLanguages;

    public DoclingParserAdapter(RestClient http, List<String> ocrLanguages) {
        this.http = http;
        this.ocrLanguages = List.copyOf(ocrLanguages);
    }

    @Override
    public ParsedDocument parse(String fileName, String mimeType, byte[] content) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("files", new NamedResource(content, fileName));
        form.add("to_formats", "md");
        form.add("do_ocr", "true");
        ocrLanguages.forEach(lang -> form.add("ocr_lang", lang));
        form.add("image_export_mode", "placeholder");
        form.add("md_page_break_placeholder", PAGE_BREAK);
        form.add("do_pdf_heading_hierarchy", "true");
        form.add("abort_on_error", "false");

        JsonNode response = http.post().uri("/v1/convert/file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve().body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Empty response from Docling Serve");
        }
        String status = response.path("status").asText();
        if ("failure".equals(status) || "skipped".equals(status)) {
            throw new IllegalStateException("Docling conversion " + status + ": " + response.path("errors"));
        }
        return toPages(response.path("document").path("md_content").asText(""));
    }

    static ParsedDocument toPages(String markdown) {
        String[] parts = markdown.split(Pattern.quote(PAGE_BREAK), -1);
        List<ParsedPage> pages = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            pages.add(new ParsedPage(i + 1, parts[i]));
        }
        return new ParsedDocument(pages);
    }

    /** Multipart file part needs a filename, or the server cannot detect the format. */
    private static final class NamedResource extends ByteArrayResource {
        private final String fileName;

        private NamedResource(byte[] content, String fileName) {
            super(content);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
