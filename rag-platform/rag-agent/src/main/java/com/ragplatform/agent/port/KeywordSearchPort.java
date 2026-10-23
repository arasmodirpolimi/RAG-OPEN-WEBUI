package com.ragplatform.agent.port;

import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;

/** Lexical (BM25) search over chunks of one collection, best first. */
public interface KeywordSearchPort {

    List<RetrievedChunk> searchByKeywords(String query, String collection, int limit);
}
