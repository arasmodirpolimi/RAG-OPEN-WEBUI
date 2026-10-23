package com.ragplatform.agent.port;

import java.util.List;

/** Cross-encoder relevance scoring of texts against a query. */
public interface RerankPort {

    /** One score per input text; {@code index} refers to the position in {@code texts}. */
    List<RerankScore> rerank(String query, List<String> texts);

    record RerankScore(int index, double score) {
    }
}
