package com.ragplatform.impl.opensearch;

import com.ragplatform.agent.port.VectorSearchPort;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;
import java.util.Map;

/** k-NN search on the HNSW (Lucene engine) field, using efficient filtering inside the knn clause. */
public final class OpenSearchVectorSearchAdapter implements VectorSearchPort {

    private final OpenSearchGateway gateway;
    private final String alias;

    public OpenSearchVectorSearchAdapter(OpenSearchGateway gateway, String alias) {
        this.gateway = gateway;
        this.alias = alias;
    }

    @Override
    public List<RetrievedChunk> searchByVector(float[] queryVector, String collection, int limit) {
        Map<String, Object> filter = Map.of("bool", Map.of(
                "filter", List.of(Map.of("term", Map.of("collection", collection)))));
        Map<String, Object> body = Map.of(
                "size", limit,
                "_source", Map.of("excludes", List.of(ChunkDocumentMapper.EMBEDDING)),
                "query", Map.of("knn", Map.of(ChunkDocumentMapper.EMBEDDING, Map.of(
                        "vector", queryVector,
                        "k", limit,
                        "filter", filter))));
        return ChunkDocumentMapper.fromSearchResponse(gateway.post("/" + alias + "/_search", body));
    }
}
