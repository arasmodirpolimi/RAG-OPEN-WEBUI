package com.ragplatform.impl.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ChunkType;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.EmbeddedChunk;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps between domain chunks and OpenSearch documents (field names live only here). */
final class ChunkDocumentMapper {

    static final String EMBEDDING = "embedding";

    private ChunkDocumentMapper() {
    }

    static Map<String, Object> toSource(EmbeddedChunk embedded) {
        Chunk c = embedded.chunk();
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("chunk_id", c.id());
        doc.put("document_id", c.documentId().toString());
        doc.put("collection", c.collection());
        doc.put("file_name", c.fileName());
        doc.put("page_start", c.pageStart());
        doc.put("page_end", c.pageEnd());
        doc.put("section_path", c.sectionPath());
        doc.put("text", c.text());
        doc.put("chunk_type", c.type().name());
        doc.put(EMBEDDING, embedded.vector());
        return doc;
    }

    static List<RetrievedChunk> fromSearchResponse(JsonNode response) {
        List<RetrievedChunk> hits = new ArrayList<>();
        if (response == null) {
            return hits;
        }
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode s = hit.path("_source");
            Chunk chunk = new Chunk(
                    s.path("chunk_id").asText(),
                    DocumentId.of(s.path("document_id").asText()),
                    s.path("collection").asText(),
                    s.path("file_name").asText(),
                    s.path("page_start").asInt(1),
                    s.path("page_end").asInt(1),
                    s.path("section_path").asText(""),
                    s.path("text").asText(""),
                    ChunkType.valueOf(s.path("chunk_type").asText(ChunkType.TEXT.name())));
            hits.add(new RetrievedChunk(chunk, hit.path("_score").asDouble()));
        }
        return hits;
    }
}
