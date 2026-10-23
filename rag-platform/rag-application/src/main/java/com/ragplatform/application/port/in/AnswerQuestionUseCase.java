package com.ragplatform.application.port.in;

import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.domain.model.ChatMessage;
import com.ragplatform.domain.model.Citation;
import java.util.List;

public interface AnswerQuestionUseCase {

    /** Streams the answer into {@code sink} and returns the sources it is based on. */
    AnswerResult answer(AskQuestionCommand command, TokenSink sink);

    record AskQuestionCommand(String collection, List<ChatMessage> conversation, String userId) {
        public AskQuestionCommand {
            conversation = List.copyOf(conversation);
            if (conversation.isEmpty()) {
                throw new IllegalArgumentException("Conversation is empty");
            }
        }
    }

    record AnswerResult(List<Citation> citations, boolean answeredFromSources) {
        public static AnswerResult notFound() {
            return new AnswerResult(List.of(), false);
        }
    }
}
