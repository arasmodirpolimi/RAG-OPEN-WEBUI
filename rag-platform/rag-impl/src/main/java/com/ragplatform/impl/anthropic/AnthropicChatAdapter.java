package com.ragplatform.impl.anthropic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragplatform.agent.port.CompletionPort;
import com.ragplatform.agent.port.StreamingCompletionPort;
import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Role;
import com.ragplatform.impl.http.RestClients;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Claude Messages API (POST /v1/messages, header anthropic-version: 2023-06-01).
 * Streaming is Server-Sent Events; text arrives in content_block_delta events with a text_delta.
 */
public final class AnthropicChatAdapter implements CompletionPort, StreamingCompletionPort {

    private static final String API_VERSION = "2023-06-01";

    private final HttpClient client = RestClients.httpClient();
    private final ObjectMapper json;
    private final URI messagesUri;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final Duration timeout;

    public AnthropicChatAdapter(ObjectMapper json, String baseUrl, String apiKey, String model,
                                int maxTokens, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("rag.anthropic.api-key is required when rag.llm.provider=anthropic");
        }
        this.json = json;
        this.messagesUri = URI.create(baseUrl.replaceAll("/+$", "") + "/v1/messages");
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeout = timeout;
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        try {
            HttpResponse<String> response = client.send(request(messages, false), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Claude API failed: HTTP " + response.statusCode() + " " + response.body());
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode block : json.readTree(response.body()).path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }
            return text.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    @Override
    public void stream(List<ChatMessage> messages, TokenSink sink) {
        try {
            HttpResponse<Stream<String>> response = client.send(request(messages, true), HttpResponse.BodyHandlers.ofLines());
            try (Stream<String> lines = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Claude API failed: HTTP " + response.statusCode() + " "
                            + lines.collect(Collectors.joining("\n")));
                }
                Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    String line = it.next();
                    if (!line.startsWith("data:")) {
                        continue; // "event:" lines and blank separators
                    }
                    JsonNode event = json.readTree(line.substring(5).trim());
                    String type = event.path("type").asText();
                    if ("content_block_delta".equals(type)
                            && "text_delta".equals(event.path("delta").path("type").asText())) {
                        sink.accept(event.path("delta").path("text").asText(""));
                    } else if ("error".equals(type)) {
                        throw new IllegalStateException("Claude stream error: " + event.path("error"));
                    } else if ("message_stop".equals(type)) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    private HttpRequest request(List<ChatMessage> messages, boolean stream) throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        String system = messages.stream().filter(m -> m.role() == Role.SYSTEM)
                .map(ChatMessage::content).collect(Collectors.joining("\n\n"));
        if (!system.isBlank()) {
            body.put("system", system);
        }
        body.put("messages", alternatingTurns(messages));
        if (stream) {
            body.put("stream", true);
        }
        return HttpRequest.newBuilder(messagesUri)
                .timeout(timeout)
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
    }

    /** The Messages API expects user/assistant turns; consecutive same-role messages are merged. */
    private static List<Map<String, String>> alternatingTurns(List<ChatMessage> messages) {
        List<Map<String, String>> turns = new ArrayList<>();
        Role previous = null;
        for (ChatMessage m : messages) {
            if (m.role() == Role.SYSTEM) {
                continue;
            }
            if (m.role() == previous) {
                Map<String, String> last = turns.get(turns.size() - 1);
                turns.set(turns.size() - 1, Map.of("role", m.role().wire(),
                        "content", last.get("content") + "\n\n" + m.content()));
            } else {
                turns.add(Map.of("role", m.role().wire(), "content", m.content()));
            }
            previous = m.role();
        }
        return turns;
    }
}
