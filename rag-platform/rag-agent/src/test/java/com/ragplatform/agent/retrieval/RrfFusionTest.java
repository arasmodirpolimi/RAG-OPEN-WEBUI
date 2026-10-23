package com.ragplatform.agent.retrieval;

import static com.ragplatform.agent.support.TestChunks.hit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class RrfFusionTest {

    private final RrfFusion rrf = new RrfFusion(60);

    @Test
    void documentFoundByBothRetrieversRanksFirst() {
        List<RetrievedChunk> bm25 = List.of(hit("a", 12.0), hit("b", 9.0), hit("c", 3.0));
        List<RetrievedChunk> knn = List.of(hit("c", 0.91), hit("d", 0.90), hit("b", 0.80));

        List<RetrievedChunk> fused = rrf.fuse(List.of(bm25, knn), 10);

        // b: 1/62 + 1/63 ; c: 1/63 + 1/61 ; a: 1/61 ; d: 1/62
        assertEquals(List.of("c", "b", "a", "d"), fused.stream().map(r -> r.chunk().id()).toList());
        assertEquals(1.0 / 63 + 1.0 / 61, fused.get(0).score(), 1e-12);
    }

    @Test
    void respectsLimitAndHandlesEmptyInput() {
        assertEquals(1, rrf.fuse(List.of(List.of(hit("a", 1), hit("b", 1))), 1).size());
        assertEquals(0, rrf.fuse(List.of(List.of(), List.of()), 5).size());
    }

    @Test
    void rejectsNonPositiveK() {
        assertThrows(IllegalArgumentException.class, () -> new RrfFusion(0));
    }
}
