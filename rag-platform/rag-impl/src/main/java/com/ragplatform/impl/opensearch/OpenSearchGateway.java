package com.ragplatform.impl.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over the OpenSearch REST API (documented, version-stable JSON contract).
 * Keeping the query DSL in plain JSON avoids coupling the code to a client library version.
 */
public final class OpenSearchGateway {

    private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");

    private final RestClient http;

    public OpenSearchGateway(RestClient http) {
        this.http = http;
    }

    public boolean exists(String path) {
        try {
            http.head().uri(path).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    public JsonNode put(String path, Object body) {
        return http.put().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode post(String path, Object body) {
        return http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode bulk(String ndjson) {
        return http.post().uri("/_bulk?refresh=wait_for").contentType(NDJSON).body(ndjson)
                .retrieve().body(JsonNode.class);
    }
}
