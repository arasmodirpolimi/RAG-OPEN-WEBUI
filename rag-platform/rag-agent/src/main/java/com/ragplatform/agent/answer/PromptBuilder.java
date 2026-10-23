package com.ragplatform.agent.answer;

import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Citation;
import com.ragplatform.domain.model.RetrievedChunk;
import com.ragplatform.domain.model.Role;
import java.util.ArrayList;
import java.util.List;

/** Builds the grounded prompt. Pure function of its inputs, so it is unit-testable. */
public final class PromptBuilder {

    static final String SYSTEM_PROMPT = """
            You answer questions using ONLY the numbered sources provided in the user message.
            Rules:
            1. Every factual statement must be supported by a source; cite it inline as [n].
            2. If the sources do not contain the answer, say clearly that the documents do not \
            contain this information. Do not use outside knowledge and do not guess.
            3. If sources conflict, say so and cite both.
            4. Answer in the same language as the question. Be concise and precise.""";

    private static final int HISTORY_MESSAGES = 4;

    public List<ChatMessage> build(String question, List<ChatMessage> conversation,
                                   List<RetrievedChunk> sources) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        messages.addAll(priorTurns(conversation));
        messages.add(ChatMessage.user(sourcesBlock(sources) + "\nQUESTION: " + question));
        return messages;
    }

    public List<Citation> citations(List<RetrievedChunk> sources) {
        List<Citation> citations = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            citations.add(Citation.of(i + 1, sources.get(i).chunk()));
        }
        return citations;
    }

    private String sourcesBlock(List<RetrievedChunk> sources) {
        StringBuilder sb = new StringBuilder("SOURCES:\n");
        List<Citation> citations = citations(sources);
        for (int i = 0; i < sources.size(); i++) {
            Citation c = citations.get(i);
            sb.append('[').append(c.number()).append("] ").append(c.fileName())
              .append(", ").append(c.pageLabel());
            if (!c.sectionPath().isBlank()) {
                sb.append(", ").append(c.sectionPath());
            }
            sb.append('\n').append(sources.get(i).chunk().text()).append("\n\n");
        }
        return sb.toString();
    }

    /** Earlier dialogue (without the last user turn and without client system prompts). */
    private static List<ChatMessage> priorTurns(List<ChatMessage> conversation) {
        List<ChatMessage> dialogue = conversation.stream().filter(m -> m.role() != Role.SYSTEM).toList();
        if (dialogue.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> withoutLast = dialogue.subList(0, dialogue.size() - 1);
        int from = Math.max(0, withoutLast.size() - HISTORY_MESSAGES);
        List<ChatMessage> prior = new ArrayList<>(withoutLast.subList(from, withoutLast.size()));
        // Chat APIs expect the dialogue to start with a user turn.
        while (!prior.isEmpty() && prior.get(0).role() != Role.USER) {
            prior.remove(0);
        }
        return prior;
    }
}
