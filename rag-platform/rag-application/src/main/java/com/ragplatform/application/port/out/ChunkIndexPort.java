package com.ragplatform.application.port.out;

import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.EmbeddedChunk;
import java.util.List;

/** Write side of the search index. */
public interface ChunkIndexPort {

    /** Removes every existing chunk of the document, then indexes the given ones (idempotent). */
    void replaceDocumentChunks(DocumentId documentId, List<EmbeddedChunk> chunks);
}
