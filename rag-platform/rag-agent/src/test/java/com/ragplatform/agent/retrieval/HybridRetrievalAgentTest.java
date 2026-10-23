package com.ragplatform.agent.retrieval;

import static com.ragplatform.agent.support.TestChunks.hit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.agent.port.EmbeddingPort;
import com.ragplatform.agent.port.KeywordSearchPort;
import com.ragplatform.agent.port.RerankPort;
import com.ragplatform.agent.port.VectorSearchPort;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class HybridRetrievalAgentTest {

    private final Executor direct = Runnable::run;
    private final EmbeddingPort embeddings = texts -> texts.stream().map(t -> new float[] {1f}).toList();
    private final KeywordSearchPort bm25 = (q, c, n) -> List.of(hit("a", 5), hit("b", 4));
    private final VectorSearchPort knn = (v, c, n) -> List.of(hit("b", 0.9), hit("c", 0.8));

    @Test
    void rerankerDecidesFinalOrderAndThresholdDropsWeakHits() {
        // fused order: b, a, c ; reranker prefers c, then a; b is below threshold
        RerankPort reranker = (q, texts) -> List.of(
                new RerankPort.RerankScore(0, 0.05), new RerankPort.RerankScore(1, 0.6),
                new RerankPort.RerankScore(2, 0.95));
        HybridRetrievalAgent agent = new HybridRetrievalAgent(embeddings, bm25, knn, reranker,
                new RrfFusion(60), new RetrievalSettings(50, 30, 5, true, 0.1), direct);

        List<RetrievedChunk> result = agent.retrieve("penale ritardo", "contratti");

        assertEquals(List.of("c", "a"), result.stream().map(r -> r.chunk().id()).toList());
        assertEquals(0.95, result.get(0).score(), 1e-9);
    }

    @Test
    void withoutRerankingFusedTopKIsReturned() {
        List<String> rerankCalls = new ArrayList<>();
        RerankPort reranker = (q, texts) -> {
            rerankCalls.add(q);
            return List.of();
        };
        HybridRetrievalAgent agent = new HybridRetrievalAgent(embeddings, bm25, knn, reranker,
                new RrfFusion(60), new RetrievalSettings(50, 30, 2, false, 0.0), direct);

        List<RetrievedChunk> result = agent.retrieve("q", "contratti");

        assertEquals(List.of("b", "a"), result.stream().map(r -> r.chunk().id()).toList());
        assertTrue(rerankCalls.isEmpty());
    }

    @Test
    void emptyIndexReturnsNothing() {
        HybridRetrievalAgent agent = new HybridRetrievalAgent(embeddings, (q, c, n) -> List.of(),
                (v, c, n) -> List.of(), (q, t) -> List.of(), new RrfFusion(60),
                new RetrievalSettings(50, 30, 5, true, 0.0), direct);
        assertTrue(agent.retrieve("q", "contratti").isEmpty());
    }
}
