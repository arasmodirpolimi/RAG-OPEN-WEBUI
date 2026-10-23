package com.ragplatform.agent.retrieval;

import com.ragplatform.domain.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion: score(d) = sum over rankings of 1 / (k + rank(d)), rank starting at 1.
 * Uses positions only, so BM25 scores and cosine similarities never need to be compared.
 */
public final class RrfFusion {

    private final int k;

    public RrfFusion(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0");
        }
        this.k = k;
    }

    public List<RetrievedChunk> fuse(List<List<RetrievedChunk>> rankings, int limit) {
        Map<String, RetrievedChunk> byId = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<RetrievedChunk> ranking : rankings) {
            for (int i = 0; i < ranking.size(); i++) {
                RetrievedChunk hit = ranking.get(i);
                String id = hit.chunk().id();
                byId.putIfAbsent(id, hit);
                scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            }
        }
        List<RetrievedChunk> fused = new ArrayList<>(scores.size());
        scores.forEach((id, score) -> fused.add(byId.get(id).withScore(score)));
        fused.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed());
        return fused.size() > limit ? List.copyOf(fused.subList(0, limit)) : List.copyOf(fused);
    }
}
