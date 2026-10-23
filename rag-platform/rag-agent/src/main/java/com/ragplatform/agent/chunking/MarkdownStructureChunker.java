package com.ragplatform.agent.chunking;

import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ChunkType;
import com.ragplatform.domain.model.ParsedDocument;
import com.ragplatform.domain.model.ParsedPage;
import com.ragplatform.domain.model.SourceDocument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structure-aware chunker for the Markdown produced by the parser.
 *
 * <ul>
 *   <li>Every heading closes the current chunk and updates the section path.</li>
 *   <li>Paragraphs are packed up to {@code maxTokens}; size-based splits carry an overlap.</li>
 *   <li>Tables are kept whole in their own chunk (split by rows, repeating the header, if too large).</li>
 *   <li>Each chunk records the page range it spans.</li>
 * </ul>
 */
public final class MarkdownStructureChunker implements DocumentChunker {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");

    private final ChunkingSettings settings;
    private final TokenEstimator tokens;
    private final RepeatedLineFilter furnitureFilter;

    public MarkdownStructureChunker(ChunkingSettings settings, TokenEstimator tokens,
                                    RepeatedLineFilter furnitureFilter) {
        this.settings = settings;
        this.tokens = tokens;
        this.furnitureFilter = furnitureFilter;
    }

    @Override
    public List<Chunk> chunk(SourceDocument document, ParsedDocument parsed) {
        Builder builder = new Builder(document);
        for (ParsedPage page : furnitureFilter.apply(parsed.pages())) {
            for (String block : splitBlocks(page.markdown())) {
                handleBlock(builder, block, page.number());
            }
        }
        builder.flush();
        return builder.chunks;
    }

    private void handleBlock(Builder b, String block, int page) {
        Matcher heading = HEADING.matcher(block.lines().findFirst().orElse(""));
        if (heading.matches() && block.lines().count() == 1) {
            b.flush();
            b.setHeading(heading.group(1).length(), heading.group(2).trim());
            return;
        }
        if (isTable(block)) {
            b.flush();
            for (String part : splitTable(block)) {
                b.emit(part, page, page, ChunkType.TABLE);
            }
            return;
        }
        for (String piece : splitOversized(block)) {
            if (b.hasContent() && b.bufferTokens + tokens.estimate(piece) > settings.maxTokens()) {
                b.flushWithOverlap();
            }
            b.append(piece, page);
        }
    }

    /** Blocks are separated by blank lines; consecutive table lines stay together. */
    private static List<String> splitBlocks(String markdown) {
        List<String> blocks = new ArrayList<>();
        for (String raw : markdown.split("\\n\\s*\\n")) {
            String block = raw.strip();
            if (block.isEmpty()) {
                continue;
            }
            // A heading glued to the following paragraph becomes two blocks.
            List<String> lines = block.lines().toList();
            if (lines.size() > 1 && HEADING.matcher(lines.get(0)).matches()) {
                blocks.add(lines.get(0));
                blocks.add(String.join("\n", lines.subList(1, lines.size())));
            } else {
                blocks.add(block);
            }
        }
        return blocks;
    }

    private static boolean isTable(String block) {
        return block.lines().allMatch(l -> l.strip().startsWith("|"));
    }

    private List<String> splitTable(String table) {
        if (tokens.estimate(table) <= settings.maxTokens()) {
            return List.of(table);
        }
        List<String> lines = table.lines().toList();
        int headerLines = lines.size() > 1 && lines.get(1).contains("---") ? 2 : 1;
        String header = String.join("\n", lines.subList(0, headerLines));
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder(header);
        for (String row : lines.subList(headerLines, lines.size())) {
            if (tokens.estimate(current + "\n" + row) > settings.maxTokens()
                    && current.length() > header.length()) {
                parts.add(current.toString());
                current = new StringBuilder(header);
            }
            current.append('\n').append(row);
        }
        parts.add(current.toString());
        return parts;
    }

    /** A single paragraph larger than maxTokens is split on sentence boundaries, then on words. */
    private List<String> splitOversized(String block) {
        if (tokens.estimate(block) <= settings.maxTokens()) {
            return List.of(block);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : block.split("(?<=[.!?;:])\\s+")) {
            for (String unit : splitWords(sentence)) {
                if (current.length() > 0
                        && tokens.estimate(current + " " + unit) > settings.maxTokens()) {
                    pieces.add(current.toString());
                    current = new StringBuilder();
                }
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(unit);
            }
        }
        if (current.length() > 0) {
            pieces.add(current.toString());
        }
        return pieces;
    }

    private List<String> splitWords(String sentence) {
        if (tokens.estimate(sentence) <= settings.maxTokens()) {
            return List.of(sentence);
        }
        String[] words = sentence.split("\\s+");
        int perPiece = Math.max(1, (int) (settings.maxTokens() / 1.3));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < words.length; i += perPiece) {
            out.add(String.join(" ", Arrays.copyOfRange(words, i, Math.min(words.length, i + perPiece))));
        }
        return out;
    }

    /** Mutable accumulation state for one document; never shared between calls. */
    private final class Builder {
        private final SourceDocument document;
        private final List<Chunk> chunks = new ArrayList<>();
        private final String[] headings = new String[6];
        private final StringBuilder buffer = new StringBuilder();
        private int bufferTokens;
        private int pageStart = -1;
        private int pageEnd = -1;

        private Builder(SourceDocument document) {
            this.document = document;
        }

        boolean hasContent() {
            return buffer.length() > 0;
        }

        void setHeading(int level, String title) {
            headings[level - 1] = title;
            for (int i = level; i < headings.length; i++) {
                headings[i] = null;
            }
        }

        String sectionPath() {
            List<String> parts = new ArrayList<>();
            for (String h : headings) {
                if (h != null && !h.isBlank()) {
                    parts.add(h);
                }
            }
            return String.join(" > ", parts);
        }

        void append(String text, int page) {
            if (pageStart < 0) {
                pageStart = page;
            }
            pageEnd = page;
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(text);
            bufferTokens = tokens.estimate(buffer.toString());
        }

        void flush() {
            if (hasContent()) {
                emit(buffer.toString(), pageStart, pageEnd, ChunkType.TEXT);
            }
            reset();
        }

        /** Size-based split: keep the tail of the closed chunk as the start of the next one. */
        void flushWithOverlap() {
            String closed = buffer.toString();
            int closedEnd = pageEnd;
            flush();
            String tail = tailWords(closed);
            if (!tail.isEmpty()) {
                append(tail, closedEnd);
            }
        }

        private String tailWords(String text) {
            int words = (int) (settings.overlapTokens() / 1.3);
            if (words <= 0) {
                return "";
            }
            String[] all = text.trim().split("\\s+");
            int from = Math.max(0, all.length - words);
            return String.join(" ", Arrays.copyOfRange(all, from, all.length));
        }

        void emit(String text, int from, int to, ChunkType type) {
            String clean = text.strip();
            if (clean.isEmpty()) {
                return;
            }
            chunks.add(new Chunk(Chunk.idFor(document.id(), chunks.size()), document.id(),
                    document.collection(), document.fileName(), from, to, sectionPath(), clean, type));
        }

        private void reset() {
            buffer.setLength(0);
            bufferTokens = 0;
            pageStart = -1;
            pageEnd = -1;
        }
    }
}
