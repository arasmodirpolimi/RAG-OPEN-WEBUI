package com.ragplatform.agent.conversation;

import com.ragplatform.agent.port.CompletionPort;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Role;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a follow-up question ("and for services?") into a standalone search query using the
 * conversation history. First questions are returned unchanged, without an LLM call.
 */
public final class QueryRewritingAgent {

    private static final System.Logger LOG = System.getLogger(QueryRewritingAgent.class.getName());
    private static final int HISTORY_MESSAGES = 6;
    private static final String INSTRUCTIONS = """
            Rewrite the user's LAST question as one standalone question that can be understood \
            without the conversation. Keep the same language as the last question. Resolve pronouns \
            and references using the conversation. Do not answer the question. \
            Output only the rewritten question, nothing else.""";

    private final CompletionPort llm;

    public QueryRewritingAgent(CompletionPort llm) {
        this.llm = llm;
    }

    public String standaloneQuestion(List<ChatMessage> conversation) {
        List<ChatMessage> dialogue = conversation.stream().filter(m -> m.role() != Role.SYSTEM).toList();
        String last = lastUserMessage(dialogue);
        long userTurns = dialogue.stream().filter(m -> m.role() == Role.USER).count();
        if (userTurns <= 1) {
            return last;
        }
        try {
            String rewritten = llm.complete(buildPrompt(dialogue)).strip();
            return rewritten.isEmpty() ? last : rewritten;
        } catch (RuntimeException e) {
            // Degrade gracefully: searching with the raw question is worse but still useful.
            LOG.log(System.Logger.Level.WARNING, "Query rewriting failed, using raw question", e);
            return last;
        }
    }

    private static List<ChatMessage> buildPrompt(List<ChatMessage> dialogue) {
        int from = Math.max(0, dialogue.size() - HISTORY_MESSAGES);
        StringBuilder transcript = new StringBuilder();
        for (ChatMessage m : dialogue.subList(from, dialogue.size())) {
            transcript.append(m.role().wire()).append(": ").append(m.content()).append('\n');
        }
        List<ChatMessage> prompt = new ArrayList<>();
        prompt.add(ChatMessage.system(INSTRUCTIONS));
        prompt.add(ChatMessage.user("Conversation:\n" + transcript));
        return prompt;
    }

    private static String lastUserMessage(List<ChatMessage> dialogue) {
        for (int i = dialogue.size() - 1; i >= 0; i--) {
            if (dialogue.get(i).role() == Role.USER) {
                return dialogue.get(i).content();
            }
        }
        throw new IllegalArgumentException("Conversation contains no user message");
    }
}
