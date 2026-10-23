package com.ragplatform.impl.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragplatform.agent.port.CompletionPort;
import com.ragplatform.agent.port.StreamingCompletionPort;
import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.impl.http.RestClients;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Ollama POST /api/chat. Streaming responses are newline-delimited JSON objects ending with done=true. */
public final class OllamaChatAdapter implements CompletionPort, StreamingCompletionPort {

    private final HttpClient client = RestClients.httpClient();
    private final ObjectMapper json;
    private final URI chatUri;
    private final String model;
    private final Duration timeout;

    public OllamaChatAdapter(ObjectMapper json, String baseUrl, String model, Duration timeout) {
        this.json = json;
        this.chatUri = URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat");
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        try {
            HttpResponse<String> response = client.send(request(messages, false), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Ollama chat failed: HTTP " + response.statusCode() + " " + response.body());
            }
            return json.readTree(response.body()).path("message").path("content").asText("");
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
                    throw new IllegalStateException("Ollama chat failed: HTTP " + response.statusCode() + " "
                            + lines.collect(Collectors.joining("\n")));
                }
                Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    String line = it.next();
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode node = json.readTree(line);
                    if (node.hasNonNull("error")) {
                        throw new IllegalStateException("Ollama error: " + node.get("error").asText());
                    }
                    String token = node.path("message").path("content").asText("");
                    if (!token.isEmpty()) {
                        sink.accept(token);
                    }
                    if (node.path("done").asBoolean(false)) {
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
        List<Map<String, String>> wire = messages.stream()
                .map(m -> Map.of("role", m.role().wire(), "content", m.content())).toList();
        String body = json.writeValueAsString(Map.of(
                "model", model,
                "messages", wire,
                "stream", stream,
                "options", Map.of("temperature", 0.1)));
        return HttpRequest.newBuilder(chatUri)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
