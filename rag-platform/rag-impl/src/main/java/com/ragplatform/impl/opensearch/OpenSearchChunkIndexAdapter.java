package com.ragplatform.impl.opensearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragplatform.application.port.out.ChunkIndexPort;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.EmbeddedChunk;
import java.util.List;
import java.util.Map;

/**
 * Replaces a document's chunks: delete_by_query on document_id, then bulk index.
 * Note: between the two steps the document is briefly not searchable (acceptable for re-indexing).
 */
public final class OpenSearchChunkIndexAdapter implements ChunkIndexPort {

    private static final int BULK_BATCH = 200;

    private final OpenSearchGateway gateway;
    private final ObjectMapper json;
    private final String alias;

    public OpenSearchChunkIndexAdapter(OpenSearchGateway gateway, ObjectMapper json, String alias) {
        this.gateway = gateway;
        this.json = json;
        this.alias = alias;
    }

    @Override
    public void replaceDocumentChunks(DocumentId documentId, List<EmbeddedChunk> chunks) {
        gateway.post("/" + alias + "/_delete_by_query?refresh=true",
                Map.of("query", Map.of("term", Map.of("document_id", documentId.toString()))));
        for (int from = 0; from < chunks.size(); from += BULK_BATCH) {
            List<EmbeddedChunk> batch = chunks.subList(from, Math.min(chunks.size(), from + BULK_BATCH));
            checkBulkResponse(gateway.bulk(toNdjson(batch)));
        }
    }

    private String toNdjson(List<EmbeddedChunk> batch) {
        StringBuilder sb = new StringBuilder();
        try {
            for (EmbeddedChunk chunk : batch) {
                sb.append(json.writeValueAsString(Map.of("index",
                        Map.of("_index", alias, "_id", chunk.chunk().id())))).append('\n');
                sb.append(json.writeValueAsString(ChunkDocumentMapper.toSource(chunk))).append('\n');
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise chunk", e);
        }
        return sb.toString();
    }

    private static void checkBulkResponse(JsonNode response) {
        if (response == null || !response.path("errors").asBoolean(false)) {
            return;
        }
        for (JsonNode item : response.path("items")) {
            JsonNode error = item.path("index").path("error");
            if (!error.isMissingNode()) {
                throw new IllegalStateException("Bulk indexing failed: " + error);
            }
        }
        throw new IllegalStateException("Bulk indexing reported errors");
    }
}
