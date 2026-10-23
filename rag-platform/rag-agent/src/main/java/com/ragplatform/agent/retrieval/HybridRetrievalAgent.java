package com.ragplatform.agent.retrieval;

import com.ragplatform.agent.port.EmbeddingPort;
import com.ragplatform.agent.port.KeywordSearchPort;
import com.ragplatform.agent.port.RerankPort;
import com.ragplatform.agent.port.RerankPort.RerankScore;
import com.ragplatform.agent.port.VectorSearchPort;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Hybrid retrieval: BM25 and vector search in parallel, RRF fusion, then cross-encoder reranking.
 * Depends only on ports, so search engine, embedding model and reranker are replaceable.
 */
public final class HybridRetrievalAgent {

    private final EmbeddingPort embeddings;
    private final KeywordSearchPort keywordSearch;
    private final VectorSearchPort vectorSearch;
    private final RerankPort reranker;
    private final RrfFusion fusion;
    private final RetrievalSettings settings;
    private final Executor executor;

    public HybridRetrievalAgent(EmbeddingPort embeddings, KeywordSearchPort keywordSearch,
                                VectorSearchPort vectorSearch, RerankPort reranker, RrfFusion fusion,
                                RetrievalSettings settings, Executor executor) {
        this.embeddings = embeddings;
        this.keywordSearch = keywordSearch;
        this.vectorSearch = vectorSearch;
        this.reranker = reranker;
        this.fusion = fusion;
        this.settings = settings;
        this.executor = executor;
    }

    public List<RetrievedChunk> retrieve(String query, String collection) {
        int n = settings.candidatesPerRetriever();
        CompletableFuture<List<RetrievedChunk>> bm25 = CompletableFuture.supplyAsync(
                () -> keywordSearch.searchByKeywords(query, collection, n), executor);
        CompletableFuture<List<RetrievedChunk>> knn = CompletableFuture.supplyAsync(
                () -> vectorSearch.searchByVector(embeddings.embed(query), collection, n), executor);

        List<RetrievedChunk> fused = fusion.fuse(List.of(join(bm25), join(knn)), settings.rerankCandidates());
        if (fused.isEmpty()) {
            return List.of();
        }
        return settings.rerankEnabled() ? rerank(query, fused) : top(fused, settings.finalTopK());
    }

    private List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates) {
        List<String> texts = candidates.stream().map(c -> c.chunk().contextualText()).toList();
        List<RerankScore> scores = new ArrayList<>(reranker.rerank(query, texts));
        scores.sort(Comparator.comparingDouble(RerankScore::score).reversed());
        List<RetrievedChunk> result = new ArrayList<>();
        for (RerankScore s : scores) {
            if (s.score() < settings.minRerankScore() || result.size() == settings.finalTopK()) {
                break;
            }
            result.add(candidates.get(s.index()).withScore(s.score()));
        }
        return List.copyOf(result);
    }

    private static List<RetrievedChunk> top(List<RetrievedChunk> list, int k) {
        return list.size() > k ? List.copyOf(list.subList(0, k)) : list;
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}
