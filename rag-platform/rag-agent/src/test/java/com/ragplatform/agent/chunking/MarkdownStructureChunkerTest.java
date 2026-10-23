package com.ragplatform.agent.chunking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ChunkType;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.ParsedDocument;
import com.ragplatform.domain.model.ParsedPage;
import com.ragplatform.domain.model.SourceDocument;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownStructureChunkerTest {

    private final SourceDocument doc = SourceDocument.newlyUploaded(DocumentId.newId(), "contratti",
            "contratto.pdf", "application/pdf", "sha", "k", Instant.EPOCH);

    private MarkdownStructureChunker chunker(int max, int overlap) {
        return new MarkdownStructureChunker(new ChunkingSettings(max, overlap),
                new WordBasedTokenEstimator(), new RepeatedLineFilter());
    }

    @Test
    void headingsStartNewChunksAndBuildSectionPath() {
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(1, """
                # Contratto di fornitura

                Premessa generale.

                ## Art. 7 - Penali

                In caso di ritardo si applica una penale dello 0,5% per giorno.
                """)));

        List<Chunk> chunks = chunker(400, 50).chunk(doc, parsed);

        assertEquals(2, chunks.size());
        assertEquals("Contratto di fornitura", chunks.get(0).sectionPath());
        assertEquals("Contratto di fornitura > Art. 7 - Penali", chunks.get(1).sectionPath());
        assertTrue(chunks.get(1).contextualText().startsWith("Contratto di fornitura > Art. 7 - Penali"));
    }

    @Test
    void tablesAreSeparateChunks() {
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(2, """
                Testo prima.

                | Giorni | Penale |
                |---|---|
                | 1-10 | 0,5% |

                Testo dopo.
                """)));

        List<Chunk> chunks = chunker(400, 50).chunk(doc, parsed);

        assertEquals(3, chunks.size());
        assertEquals(ChunkType.TABLE, chunks.get(1).type());
        assertEquals(2, chunks.get(1).pageStart());
    }

    @Test
    void sizeSplitsCarryOverlapAndTrackPages() {
        String paragraph = "parola ".repeat(60).trim();
        ParsedDocument parsed = new ParsedDocument(List.of(
                new ParsedPage(1, paragraph), new ParsedPage(2, paragraph), new ParsedPage(3, paragraph)));

        List<Chunk> chunks = chunker(100, 20).chunk(doc, parsed);

        assertTrue(chunks.size() >= 2);
        assertEquals(1, chunks.get(0).pageStart());
        Chunk second = chunks.get(1);
        assertEquals(1, second.pageStart(), "overlap starts on the page where the previous chunk ended");
        assertTrue(second.pageEnd() >= 2);
    }

    @Test
    void chunkIdsAreDeterministic() {
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(1, "Uno.\n\n# Due\n\nTre.")));
        List<Chunk> first = chunker(400, 50).chunk(doc, parsed);
        List<Chunk> second = chunker(400, 50).chunk(doc, parsed);
        assertEquals(first.stream().map(Chunk::id).toList(), second.stream().map(Chunk::id).toList());
        assertEquals(doc.id() + "-0", first.get(0).id());
    }

    @Test
    void oversizedParagraphIsSplitBelowLimit() {
        String sentence = "Questa è una frase di prova abbastanza lunga. ";
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(1, sentence.repeat(40))));
        WordBasedTokenEstimator est = new WordBasedTokenEstimator();

        List<Chunk> chunks = chunker(100, 0).chunk(doc, parsed);

        assertTrue(chunks.size() > 1);
        chunks.forEach(c -> assertFalse(est.estimate(c.text()) > 100, "chunk exceeds max tokens"));
    }
}
