package com.ragplatform.impl.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.ragplatform.agent.port.EmbeddingPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Ollama POST /api/embed (batch input). Fails fast if the model's dimension differs from the index. */
public final class OllamaEmbeddingAdapter implements EmbeddingPort {

    private final RestClient http;
    private final String model;
    private final int expectedDimension;

    public OllamaEmbeddingAdapter(RestClient http, String model, int expectedDimension) {
        this.http = http;
        this.model = model;
        this.expectedDimension = expectedDimension;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        JsonNode response = http.post().uri("/api/embed").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "input", texts))
                .retrieve().body(JsonNode.class);
        JsonNode embeddings = response == null ? null : response.get("embeddings");
        if (embeddings == null || !embeddings.isArray() || embeddings.size() != texts.size()) {
            throw new IllegalStateException("Unexpected /api/embed response for " + texts.size() + " inputs");
        }
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (JsonNode values : embeddings) {
            if (values.size() != expectedDimension) {
                throw new IllegalStateException("Model '" + model + "' returned dimension " + values.size()
                        + " but the index expects " + expectedDimension);
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) values.get(i).asDouble();
            }
            vectors.add(vector);
        }
        return vectors;
    }
}
