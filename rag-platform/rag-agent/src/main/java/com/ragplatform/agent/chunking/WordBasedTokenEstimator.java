package com.ragplatform.agent.chunking;

/** Heuristic: about 1.3 tokens per whitespace-separated word. Good enough for sizing chunks. */
public final class WordBasedTokenEstimator implements TokenEstimator {

    private static final double TOKENS_PER_WORD = 1.3;

    @Override
    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int words = text.trim().split("\\s+").length;
        return (int) Math.ceil(words * TOKENS_PER_WORD);
    }
}
