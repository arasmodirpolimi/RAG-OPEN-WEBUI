package com.ragplatform.agent.port;

import com.ragplatform.domain.model.ChatMessage;
import java.util.List;

/** Single, non-streamed LLM completion (used for short internal tasks such as query rewriting). */
public interface CompletionPort {

    String complete(List<ChatMessage> messages);
}
