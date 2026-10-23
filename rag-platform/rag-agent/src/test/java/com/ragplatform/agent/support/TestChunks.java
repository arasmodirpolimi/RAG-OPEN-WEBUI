package com.ragplatform.agent.support;

import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ChunkType;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.UUID;

public final class TestChunks {

    private static final DocumentId DOC = new DocumentId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private TestChunks() {
    }

    public static RetrievedChunk hit(String id, double score) {
        return new RetrievedChunk(chunk(id, "text of " + id), score);
    }

    public static Chunk chunk(String id, String text) {
        return new Chunk(id, DOC, "contratti", "contratto.pdf", 6, 6, "Art. 7 > Penali", text, ChunkType.TEXT);
    }
}
