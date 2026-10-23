package com.ragplatform.domain.model;

import java.util.Objects;

public record EmbeddedChunk(Chunk chunk, float[] vector) {

    public EmbeddedChunk {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(vector, "vector");
    }
}
