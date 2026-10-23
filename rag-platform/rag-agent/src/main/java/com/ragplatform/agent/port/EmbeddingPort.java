package com.ragplatform.agent.port;

import java.util.List;

/** Turns texts into dense vectors. The same model must be used for documents and queries. */
public interface EmbeddingPort {

    List<float[]> embed(List<String> texts);

    default float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }
}
