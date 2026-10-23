package com.ragplatform.api.web.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ragplatform.application.port.in.AnswerQuestionUseCase;
import com.ragplatform.application.port.in.AnswerQuestionUseCase.AnswerResult;
import com.ragplatform.application.port.in.AnswerQuestionUseCase.AskQuestionCommand;
import com.ragplatform.application.port.in.ListCollectionsUseCase;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Role;
import com.ragplatform.impl.config.RagProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * OpenAI-compatible endpoints consumed by Open WebUI. Each collection is exposed as a model
 * named "<prefix><collection>" (e.g. "rag-contratti"); the selected model chooses the collection.
 *
 * <p>chat() always returns ResponseEntity&lt;StreamingResponseBody&gt;: Spring MVC selects its streaming
 * handler from the declared generic type, so streamed, blocking and error responses share that type.
 */
@RestController
@RequestMapping("/v1")
public class OpenAiCompatibleController {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleController.class);
    private static final String USER_EMAIL_HEADER = "X-OpenWebUI-User-Email";

    private final AnswerQuestionUseCase answers;
    private final ListCollectionsUseCase collections;
    private final ObjectMapper json;
    private final String modelPrefix;

    public OpenAiCompatibleController(AnswerQuestionUseCase answers, ListCollectionsUseCase collections,
                                      ObjectMapper json, RagProperties props) {
        this.answers = answers;
        this.collections = collections;
        this.json = json;
        this.modelPrefix = props.answer().modelPrefix();
    }

    @GetMapping("/models")
    public Map<String, Object> models() {
        long created = Instant.now().getEpochSecond();
        List<Map<String, Object>> data = collections.collections().stream()
                .map(c -> Map.<String, Object>of("id", modelPrefix + c, "object", "model",
                        "created", created, "owned_by", "rag-platform"))
                .toList();
        return Map.of("object", "list", "data", data);
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<StreamingResponseBody> chat(
            @RequestBody ChatCompletionRequest request,
            @RequestHeader(value = USER_EMAIL_HEADER, required = false) String userEmail) {
        String model = request.model() == null ? "" : request.model();
        if (!model.startsWith(modelPrefix)
                || !collections.collections().contains(model.substring(modelPrefix.length()))) {
            return error(HttpStatus.NOT_FOUND, "Unknown model: " + model);
        }
        List<ChatMessage> conversation = ChatRequestMapper.toDomain(request);
        if (conversation.stream().noneMatch(m -> m.role() == Role.USER)) {
            return error(HttpStatus.BAD_REQUEST, "messages must contain at least one user message");
        }
        String collection = model.substring(modelPrefix.length());
        AskQuestionCommand command = new AskQuestionCommand(collection, conversation, userEmail);
        LOG.info("question collection={} user={}", collection, userEmail == null ? "-" : userEmail);

        String id = "chatcmpl-" + UUID.randomUUID();
        long created = Instant.now().getEpochSecond();
        return request.isStreaming()
                ? streaming(command, id, created, model)
                : blocking(command, id, created, model);
    }

    private ResponseEntity<StreamingResponseBody> streaming(AskQuestionCommand command, String id,
                                                             long created, String model) {
        StreamingResponseBody body = out -> {
            OpenAiSseWriter sse = new OpenAiSseWriter(out, json, id, created, model);
            sse.role();
            try {
                AnswerResult result = answers.answer(command, sse::content);
                sse.content(SourcesFormatter.format(result.citations()));
            } catch (RuntimeException e) {
                LOG.error("Answer failed for {}", id, e);
                sse.content("\n\n[Errore durante la generazione della risposta / Error while generating the answer]");
            }
            sse.finish();
            sse.done();
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    private ResponseEntity<StreamingResponseBody> blocking(AskQuestionCommand command, String id,
                                                            long created, String model) {
        StreamingResponseBody body = out -> {
            StringBuilder text = new StringBuilder();
            AnswerResult result = answers.answer(command, text::append);
            text.append(SourcesFormatter.format(result.citations()));
            ObjectNode root = json.createObjectNode()
                    .put("id", id).put("object", "chat.completion").put("created", created).put("model", model);
            ObjectNode choice = root.putArray("choices").addObject().put("index", 0).put("finish_reason", "stop");
            choice.putObject("message").put("role", "assistant").put("content", text.toString());
            out.write(json.writeValueAsBytes(root));
        };
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<StreamingResponseBody> error(HttpStatus status, String message) {
        Map<String, Object> payload = Map.of("error", Map.of("message", message, "type", "invalid_request_error"));
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(out -> out.write(json.writeValueAsBytes(payload)));
    }
}
