package com.ragplatform.impl.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All platform settings, bound from rag-common.yml / environment variables (RAG_*). */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        List<String> collections,
        OpenSearch opensearch,
        Ollama ollama,
        Llm llm,
        Anthropic anthropic,
        Reranker reranker,
        Retrieval retrieval,
        Chunking chunking,
        Docling docling,
        Storage storage,
        Queue queue,
        Security security,
        Answer answer) {

    public record OpenSearch(String url, String indexName, String indexAlias, int dimension, String analyzer) {
    }

    public record Ollama(String url, String embeddingModel, String chatModel, Duration timeout) {
    }

    /** provider: "ollama" (local, default) or "anthropic". */
    public record Llm(String provider) {
    }

    public record Anthropic(String url, String apiKey, String model, int maxTokens, Duration timeout) {
    }

    public record Reranker(boolean enabled, String url, double minScore) {
    }

    public record Retrieval(int candidatesPerRetriever, int rerankCandidates, int finalTopK, int rrfK) {
    }

    public record Chunking(int maxTokens, int overlapTokens, int embeddingBatchSize) {
    }

    public record Docling(String url, Duration timeout, List<String> ocrLanguages) {
    }

    public record Storage(String root) {
    }

    public record Queue(String ingest, String deadLetter) {
    }

    public record Security(String openaiApiKey, String adminApiKey) {
    }

    public record Answer(String notFoundMessage, String modelPrefix) {
    }
}
