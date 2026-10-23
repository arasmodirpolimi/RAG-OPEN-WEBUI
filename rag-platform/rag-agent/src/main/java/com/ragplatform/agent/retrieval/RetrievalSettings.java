package com.ragplatform.agent.retrieval;

/**
 * @param candidatesPerRetriever hits requested from BM25 and from vector search
 * @param rerankCandidates       fused hits sent to the reranker
 * @param finalTopK              hits passed to the LLM
 * @param rerankEnabled          when false, the fused ranking is used directly
 * @param minRerankScore         hits scoring below this are dropped (0 disables; calibrate on your data)
 */
public record RetrievalSettings(int candidatesPerRetriever, int rerankCandidates, int finalTopK,
                                boolean rerankEnabled, double minRerankScore) {

    public RetrievalSettings {
        if (candidatesPerRetriever < 1 || rerankCandidates < 1 || finalTopK < 1) {
            throw new IllegalArgumentException("retrieval sizes must be >= 1");
        }
        if (finalTopK > rerankCandidates) {
            throw new IllegalArgumentException("finalTopK must be <= rerankCandidates");
        }
    }
}
