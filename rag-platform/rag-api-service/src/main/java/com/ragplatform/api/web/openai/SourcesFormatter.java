package com.ragplatform.api.web.openai;

import com.ragplatform.domain.model.Citation;
import java.util.List;

/** Renders the citation list appended under each answer (Markdown, displayed by Open WebUI). */
final class SourcesFormatter {

    private SourcesFormatter() {
    }

    static String format(List<Citation> citations) {
        if (citations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n---\n**Fonti / Sources**\n");
        for (Citation c : citations) {
            sb.append("\n[").append(c.number()).append("] ").append(c.fileName()).append(", ").append(c.pageLabel());
            if (!c.sectionPath().isBlank()) {
                sb.append(" — ").append(c.sectionPath());
            }
            sb.append("  ");
        }
        return sb.toString();
    }
}
