package com.ragplatform.api.web.openai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Subset of the OpenAI chat-completions request that this endpoint uses; other fields are ignored. */
public record ChatCompletionRequest(String model, List<Message> messages, Boolean stream) {

    /** content is a string, or an array of parts like [{"type":"text","text":"..."}]. */
    public record Message(String role, JsonNode content) {
    }

    public boolean isStreaming() {
        return stream == null || stream;
    }
}
