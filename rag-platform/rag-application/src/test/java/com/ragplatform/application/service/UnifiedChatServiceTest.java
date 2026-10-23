package com.ragplatform.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.agent.answer.AnswerGenerationAgent;
import com.ragplatform.agent.answer.PromptBuilder;
import com.ragplatform.agent.conversation.QueryRewritingAgent;
import com.ragplatform.agent.port.KeywordSearchPort;
import com.ragplatform.agent.retrieval.HybridRetrievalAgent;
import com.ragplatform.agent.retrieval.RetrievalSettings;
import com.ragplatform.agent.retrieval.RrfFusion;
import com.ragplatform.application.port.in.AnswerQuestionUseCase.AnswerResult;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ChunkType;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UnifiedChatServiceTest {

    private UnifiedChatService service(KeywordSearchPort bm25) {
        HybridRetrievalAgent retriever = new HybridRetrievalAgent(
                t -> t.stream().map(x -> new float[]{1f}).toList(), bm25, (v, c, n) -> List.of(),
                (q, texts) -> List.of(), new RrfFusion(60), new RetrievalSettings(10, 5, 3, false, 0), Runnable::run);
        AnswerGenerationAgent generator = new AnswerGenerationAgent((messages, sink) ->
                sink.accept("Grounded answer [1]."), new PromptBuilder());
        return new UnifiedChatService(new QueryRewritingAgent(m -> "unused"), retriever, generator,
                (messages, sink) -> sink.accept("Hello! How can I help?"),
                new CollectionPolicy(List.of("contratti")), 6);
    }

    @Test
    void answersFromSourcesWhenDocumentsExist() {
        Chunk chunk = new Chunk("c1", new DocumentId(UUID.randomUUID()), "contratti", "c.pdf", 3, 3,
                "Art. 1", "Content here", ChunkType.TEXT);
        StringBuilder out = new StringBuilder();

        AnswerResult result = service((q, c, n) -> List.of(new RetrievedChunk(chunk, 3.0)))
                .chat(List.of(ChatMessage.user("What does Art. 1 say?")), out::append);

        assertTrue(result.answeredFromSources());
        assertFalse(result.citations().isEmpty());
        assertEquals("Grounded answer [1].", out.toString());
    }

    @Test
    void fallsBackToLlmWhenNoDocuments() {
        StringBuilder out = new StringBuilder();

        AnswerResult result = service((q, c, n) -> List.of())
                .chat(List.of(ChatMessage.user("Hi")), out::append);

        assertFalse(result.answeredFromSources());
        assertTrue(result.citations().isEmpty());
        assertEquals("Hello! How can I help?", out.toString());
    }
}
