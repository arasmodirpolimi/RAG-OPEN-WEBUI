package com.ragplatform.agent.chunking;

import com.ragplatform.domain.model.ParsedPage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes short lines that repeat on most pages (running headers/footers such as
 * "Pagina 3 di 20" variants are not identical, but letterheads and fixed footers are).
 * Such lines add noise to BM25 and embeddings without carrying content.
 */
public final class RepeatedLineFilter {

    private static final int MIN_PAGES = 3;
    private static final int MAX_LINE_LENGTH = 120;
    private static final double MIN_PAGE_SHARE = 0.5;

    public List<ParsedPage> apply(List<ParsedPage> pages) {
        if (pages.size() < MIN_PAGES) {
            return pages;
        }
        Set<String> repeated = findRepeatedLines(pages);
        if (repeated.isEmpty()) {
            return pages;
        }
        return pages.stream()
                .map(p -> new ParsedPage(p.number(), removeLines(p.markdown(), repeated)))
                .toList();
    }

    private Set<String> findRepeatedLines(List<ParsedPage> pages) {
        Map<String, Integer> pageCounts = new HashMap<>();
        for (ParsedPage page : pages) {
            Set<String> seenOnPage = new HashSet<>();
            for (String line : page.markdown().split("\n")) {
                String trimmed = line.trim();
                if (isCandidate(trimmed) && seenOnPage.add(trimmed)) {
                    pageCounts.merge(trimmed, 1, Integer::sum);
                }
            }
        }
        int threshold = (int) Math.ceil(pages.size() * MIN_PAGE_SHARE);
        Set<String> repeated = new HashSet<>();
        pageCounts.forEach((line, count) -> {
            if (count >= threshold) {
                repeated.add(line);
            }
        });
        return repeated;
    }

    private static boolean isCandidate(String line) {
        // Headings and table rows are content, never furniture.
        return !line.isEmpty() && line.length() <= MAX_LINE_LENGTH
                && !line.startsWith("#") && !line.startsWith("|");
    }

    private static String removeLines(String markdown, Set<String> repeated) {
        StringBuilder out = new StringBuilder();
        for (String line : markdown.split("\n", -1)) {
            if (!repeated.contains(line.trim())) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }
}
