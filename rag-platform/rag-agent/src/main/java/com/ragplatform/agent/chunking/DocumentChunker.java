package com.ragplatform.agent.chunking;

import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ParsedDocument;
import com.ragplatform.domain.model.SourceDocument;
import java.util.List;

public interface DocumentChunker {

    List<Chunk> chunk(SourceDocument document, ParsedDocument parsed);
}
