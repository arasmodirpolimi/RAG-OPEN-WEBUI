package com.ragplatform.application.port.in;

import com.ragplatform.agent.port.TokenSink;
import com.ragplatform.application.port.in.AnswerQuestionUseCase.AnswerResult;
import com.ragplatform.domain.model.ChatMessage;
import java.util.List;

/**
 * Single model that searches all collections for relevant sources. If found, answers with
 * grounded citations; otherwise falls back to a direct LLM conversation.
 */
public interface UnifiedChatUseCase {

    AnswerResult chat(List<ChatMessage> conversation, TokenSink sink);
}
