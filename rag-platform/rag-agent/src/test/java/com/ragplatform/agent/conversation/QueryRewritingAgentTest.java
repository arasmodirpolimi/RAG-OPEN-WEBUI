package com.ragplatform.agent.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.domain.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryRewritingAgentTest {

    @Test
    void firstQuestionIsNotRewritten() {
        List<String> calls = new ArrayList<>();
        QueryRewritingAgent agent = new QueryRewritingAgent(m -> {
            calls.add("called");
            return "x";
        });
        assertEquals("Qual è la penale?", agent.standaloneQuestion(List.of(
                ChatMessage.system("client system prompt"), ChatMessage.user("Qual è la penale?"))));
        assertTrue(calls.isEmpty());
    }

    @Test
    void followUpIsRewrittenWithHistory() {
        QueryRewritingAgent agent = new QueryRewritingAgent(m -> {
            assertTrue(m.get(1).content().contains("Qual è la penale?"));
            return "  Qual è la penale per i contratti di servizi?  ";
        });
        String q = agent.standaloneQuestion(List.of(ChatMessage.user("Qual è la penale?"),
                ChatMessage.assistant("0,5% al giorno [1]"), ChatMessage.user("E per i servizi?")));
        assertEquals("Qual è la penale per i contratti di servizi?", q);
    }

    @Test
    void fallsBackToRawQuestionWhenLlmFails() {
        QueryRewritingAgent agent = new QueryRewritingAgent(m -> {
            throw new IllegalStateException("llm down");
        });
        assertEquals("E per i servizi?", agent.standaloneQuestion(List.of(ChatMessage.user("Qual è la penale?"),
                ChatMessage.assistant("..."), ChatMessage.user("E per i servizi?"))));
    }
}
