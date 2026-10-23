package com.ragplatform.impl.opensearch;

import com.ragplatform.agent.port.KeywordSearchPort;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;
import java.util.Map;

/** BM25 search (OpenSearch default similarity) with the collection as a non-scoring filter. */
public final class OpenSearchKeywordSearchAdapter implements KeywordSearchPort {

    private final OpenSearchGateway gateway;
    private final String alias;

    public OpenSearchKeywordSearchAdapter(OpenSearchGateway gateway, String alias) {
        this.gateway = gateway;
        this.alias = alias;
    }

    @Override
    public List<RetrievedChunk> searchByKeywords(String query, String collection, int limit) {
        Map<String, Object> body = Map.of(
                "size", limit,
                "_source", Map.of("excludes", List.of(ChunkDocumentMapper.EMBEDDING)),
                "query", Map.of("bool", Map.of(
                        "must", List.of(Map.of("multi_match", Map.of(
                                "query", query,
                                "fields", List.of("text", "text.en", "section_path^2")))),
                        "filter", List.of(Map.of("term", Map.of("collection", collection))))));
        return ChunkDocumentMapper.fromSearchResponse(gateway.post("/" + alias + "/_search", body));
    }
}
