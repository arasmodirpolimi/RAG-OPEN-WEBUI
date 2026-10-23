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
import com.ragplatform.application.port.in.AnswerQuestionUseCase.AskQuestionCommand;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Chunk;
import com.ragplatform.domain.model.ChunkType;
import com.ragplatform.domain.model.DocumentId;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnswerQuestionServiceTest {

    private final List<String> llmCalls = new ArrayList<>();

    private AnswerQuestionService service(KeywordSearchPort bm25) {
        HybridRetrievalAgent retriever = new HybridRetrievalAgent(
                t -> t.stream().map(x -> new float[] {1f}).toList(), bm25, (v, c, n) -> List.of(),
                (q, texts) -> List.of(), new RrfFusion(60), new RetrievalSettings(10, 5, 3, false, 0), Runnable::run);
        AnswerGenerationAgent generator = new AnswerGenerationAgent((messages, sink) -> {
            llmCalls.add(messages.get(messages.size() - 1).content());
            sink.accept("La penale è 0,5% [1].");
        }, new PromptBuilder());
        return new AnswerQuestionService(new QueryRewritingAgent(m -> "unused"), retriever, generator,
                new CollectionPolicy(List.of("contratti")), "Not found in the documents.");
    }

    @Test
    void answersWithCitationsWhenSourcesExist() {
        Chunk chunk = new Chunk("c1", new DocumentId(UUID.randomUUID()), "contratti", "c.pdf", 6, 6,
                "Art. 7", "Penale 0,5%", ChunkType.TEXT);
        StringBuilder out = new StringBuilder();

        AnswerResult result = service((q, c, n) -> List.of(new RetrievedChunk(chunk, 3.0)))
                .answer(new AskQuestionCommand("contratti", List.of(ChatMessage.user("Qual è la penale?")), "u"),
                        out::append);

        assertTrue(result.answeredFromSources());
        assertEquals("c.pdf", result.citations().get(0).fileName());
        assertEquals("La penale è 0,5% [1].", out.toString());
    }

    @Test
    void neverCallsLlmWithoutSources() {
        StringBuilder out = new StringBuilder();

        AnswerResult result = service((q, c, n) -> List.of())
                .answer(new AskQuestionCommand("contratti", List.of(ChatMessage.user("?")), "u"), out::append);

        assertFalse(result.answeredFromSources());
        assertEquals("Not found in the documents.", out.toString());
        assertTrue(llmCalls.isEmpty());
    }
}
