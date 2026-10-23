package com.ragplatform.agent.chunking;

/** Estimates token counts; swap in a real tokenizer without touching the chunker (DIP). */
@FunctionalInterface
public interface TokenEstimator {

    int estimate(String text);
}
