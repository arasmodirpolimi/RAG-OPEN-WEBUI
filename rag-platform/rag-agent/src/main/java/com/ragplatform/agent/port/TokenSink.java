package com.ragplatform.agent.port;

/** Receives streamed text fragments. */
@FunctionalInterface
public interface TokenSink {

    void accept(String token);
}
