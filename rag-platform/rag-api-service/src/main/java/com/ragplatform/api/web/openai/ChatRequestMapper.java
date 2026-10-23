package com.ragplatform.api.web.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Translates the OpenAI wire format into domain messages (anti-corruption layer). */
final class ChatRequestMapper {

    private ChatRequestMapper() {
    }

    static List<ChatMessage> toDomain(ChatCompletionRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        if (request.messages() == null) {
            return messages;
        }
        for (ChatCompletionRequest.Message m : request.messages()) {
            if (m.role() == null) {
                continue;
            }
            String role = m.role().toLowerCase(Locale.ROOT);
            if (!role.equals("system") && !role.equals("user") && !role.equals("assistant")) {
                continue; // tool/function messages are not used by this pipeline
            }
            messages.add(new ChatMessage(Role.fromWire(role), text(m.content())));
        }
        return messages;
    }

    private static String text(JsonNode content) {
        if (content == null || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText();
        }
        StringBuilder sb = new StringBuilder();
        if (content.isArray()) {
            for (JsonNode part : content) {
                if ("text".equals(part.path("type").asText())) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(part.path("text").asText());
                }
            }
        }
        return sb.toString();
    }
}
