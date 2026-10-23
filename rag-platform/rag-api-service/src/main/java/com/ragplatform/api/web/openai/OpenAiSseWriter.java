package com.ragplatform.api.web.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes OpenAI-style streaming chunks ("data: {chat.completion.chunk}\n\n", then "data: [DONE]\n\n")
 * directly to the response, flushing after each event so tokens reach the browser immediately.
 */
final class OpenAiSseWriter {

    private final OutputStream out;
    private final ObjectMapper json;
    private final String id;
    private final long created;
    private final String model;

    OpenAiSseWriter(OutputStream out, ObjectMapper json, String id, long created, String model) {
        this.out = out;
        this.json = json;
        this.id = id;
        this.created = created;
        this.model = model;
    }

    void role() {
        ObjectNode delta = json.createObjectNode().put("role", "assistant");
        write(chunk(delta, null));
    }

    void content(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        write(chunk(json.createObjectNode().put("content", text), null));
    }

    void finish() {
        write(chunk(json.createObjectNode(), "stop"));
    }

    void done() {
        raw("data: [DONE]\n\n");
    }

    private ObjectNode chunk(ObjectNode delta, String finishReason) {
        ObjectNode root = json.createObjectNode()
                .put("id", id).put("object", "chat.completion.chunk").put("created", created).put("model", model);
        ObjectNode choice = root.putArray("choices").addObject().put("index", 0);
        choice.set("delta", delta);
        if (finishReason == null) {
            choice.putNull("finish_reason");
        } else {
            choice.put("finish_reason", finishReason);
        }
        return root;
    }

    private void write(ObjectNode node) {
        try {
            raw("data: " + json.writeValueAsString(node) + "\n\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void raw(String s) {
        try {
            out.write(s.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            // client disconnected: propagate to stop the LLM stream
            throw new UncheckedIOException(e);
        }
    }
}
