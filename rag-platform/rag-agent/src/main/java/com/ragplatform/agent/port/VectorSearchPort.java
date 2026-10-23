package com.ragplatform.agent.port;

import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;

/** Approximate nearest-neighbour search over chunk embeddings of one collection, best first. */
public interface VectorSearchPort {

    List<RetrievedChunk> searchByVector(float[] queryVector, String collection, int limit);
}
