package com.ragplatform.agent.chunking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.domain.model.ParsedPage;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepeatedLineFilterTest {

    @Test
    void removesLinesRepeatedOnMostPagesButKeepsHeadings() {
        List<ParsedPage> pages = List.of(
                new ParsedPage(1, "Comune di Esempio - Ufficio Contratti\n# Titolo\nContenuto uno"),
                new ParsedPage(2, "Comune di Esempio - Ufficio Contratti\n# Titolo\nContenuto due"),
                new ParsedPage(3, "Comune di Esempio - Ufficio Contratti\n# Titolo\nContenuto tre"));

        List<ParsedPage> filtered = new RepeatedLineFilter().apply(pages);

        filtered.forEach(p -> {
            assertFalse(p.markdown().contains("Ufficio Contratti"));
            assertTrue(p.markdown().contains("# Titolo"));
        });
    }
}
