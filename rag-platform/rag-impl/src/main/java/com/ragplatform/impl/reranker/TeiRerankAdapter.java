package com.ragplatform.impl.reranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.ragplatform.agent.port.RerankPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Hugging Face Text Embeddings Inference: POST /rerank {query, texts} -> [{index, score}]. */
public final class TeiRerankAdapter implements RerankPort {

    private final RestClient http;

    public TeiRerankAdapter(RestClient http) {
        this.http = http;
    }

    @Override
    public List<RerankScore> rerank(String query, List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        JsonNode response = http.post().uri("/rerank").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query, "texts", texts, "truncate", true))
                .retrieve().body(JsonNode.class);
        if (response == null || !response.isArray()) {
            throw new IllegalStateException("Unexpected /rerank response");
        }
        List<RerankScore> scores = new ArrayList<>(response.size());
        for (JsonNode rank : response) {
            scores.add(new RerankScore(rank.path("index").asInt(), rank.path("score").asDouble()));
        }
        return scores;
    }
}
