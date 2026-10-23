package com.ragplatform.agent.chunking;

public record ChunkingSettings(int maxTokens, int overlapTokens) {

    public ChunkingSettings {
        if (maxTokens < 50) {
            throw new IllegalArgumentException("maxTokens must be >= 50");
        }
        if (overlapTokens < 0 || overlapTokens >= maxTokens / 2) {
            throw new IllegalArgumentException("overlapTokens must be in [0, maxTokens/2)");
        }
    }
}
