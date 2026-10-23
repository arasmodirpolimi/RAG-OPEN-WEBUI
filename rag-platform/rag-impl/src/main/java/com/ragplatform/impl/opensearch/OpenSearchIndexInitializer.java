package com.ragplatform.impl.opensearch;

import com.ragplatform.impl.config.RagProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the versioned chunk index and its alias if missing. All reads and writes go through the
 * alias, so a new index (new embedding model, new analyzer) can be built and swapped in later.
 */
public final class OpenSearchIndexInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchIndexInitializer.class);

    private final OpenSearchGateway gateway;
    private final RagProperties.OpenSearch config;

    public OpenSearchIndexInitializer(OpenSearchGateway gateway, RagProperties.OpenSearch config) {
        this.gateway = gateway;
        this.config = config;
    }

    public void ensureIndex() {
        if (gateway.exists("/" + config.indexAlias())) {
            LOG.info("OpenSearch alias '{}' already exists", config.indexAlias());
            return;
        }
        Map<String, Object> textField = Map.of("type", "text", "analyzer", config.analyzer(),
                "fields", Map.of("en", Map.of("type", "text", "analyzer", "english")));
        Map<String, Object> body = Map.of(
                "settings", Map.of("index", Map.of("knn", true, "number_of_shards", 1, "number_of_replicas", 0)),
                "mappings", Map.of("properties", Map.of(
                        "chunk_id", Map.of("type", "keyword"),
                        "document_id", Map.of("type", "keyword"),
                        "collection", Map.of("type", "keyword"),
                        "file_name", Map.of("type", "keyword"),
                        "page_start", Map.of("type", "integer"),
                        "page_end", Map.of("type", "integer"),
                        "chunk_type", Map.of("type", "keyword"),
                        "section_path", Map.of("type", "text", "analyzer", config.analyzer()),
                        "text", textField,
                        ChunkDocumentMapper.EMBEDDING, Map.of(
                                "type", "knn_vector",
                                "dimension", config.dimension(),
                                "method", Map.of("name", "hnsw", "space_type", "cosinesimil", "engine", "lucene")))),
                "aliases", Map.of(config.indexAlias(), Map.of()));
        gateway.put("/" + config.indexName(), body);
        LOG.info("Created OpenSearch index '{}' with alias '{}'", config.indexName(), config.indexAlias());
    }
}
