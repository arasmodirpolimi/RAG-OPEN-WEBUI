package com.ragplatform.agent.answer;

import com.ragplatform.agent.port.StreamingCompletionPort;
import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Citation;
import com.ragplatform.domain.model.RetrievedChunk;
import java.util.List;

/** Streams a grounded answer and returns the citations the [n] markers refer to. */
public final class AnswerGenerationAgent {

    private final StreamingCompletionPort llm;
    private final PromptBuilder prompts;

    public AnswerGenerationAgent(StreamingCompletionPort llm, PromptBuilder prompts) {
        this.llm = llm;
        this.prompts = prompts;
    }

    public List<Citation> generate(String question, List<ChatMessage> conversation,
                                   List<RetrievedChunk> sources, TokenSink sink) {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("generate() requires at least one source");
        }
        llm.stream(prompts.build(question, conversation, sources), sink);
        return prompts.citations(sources);
    }
}
