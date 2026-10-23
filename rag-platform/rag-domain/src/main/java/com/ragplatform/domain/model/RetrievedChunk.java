package com.ragplatform.domain.model;

/** A chunk returned by a retriever, with the score of the stage that produced it. */
public record RetrievedChunk(Chunk chunk, double score) {

    public RetrievedChunk withScore(double newScore) {
        return new RetrievedChunk(chunk, newScore);
    }
}
