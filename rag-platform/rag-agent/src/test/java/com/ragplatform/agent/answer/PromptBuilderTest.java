package com.ragplatform.agent.answer;

import static com.ragplatform.agent.support.TestChunks.chunk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.RetrievedChunk;
import com.ragplatform.domain.model.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void numbersSourcesAndEndsWithQuestion() {
        List<RetrievedChunk> sources = List.of(new RetrievedChunk(chunk("a", "Penale 0,5% al giorno."), 0.9));
        List<ChatMessage> prompt = builder.build("Qual è la penale?", List.of(
                ChatMessage.system("ignored client prompt"), ChatMessage.assistant("orphan"),
                ChatMessage.user("Qual è la penale?")), sources);

        assertEquals(Role.SYSTEM, prompt.get(0).role());
        assertEquals(2, prompt.size(), "client system prompt and leading assistant turn are dropped");
        String user = prompt.get(1).content();
        assertTrue(user.contains("[1] contratto.pdf, p. 6, Art. 7 > Penali"));
        assertTrue(user.endsWith("QUESTION: Qual è la penale?"));
        assertEquals("p. 6", builder.citations(sources).get(0).pageLabel());
    }
}
