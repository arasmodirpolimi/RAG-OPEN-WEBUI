package com.ragplatform.agent.port;

import com.ragplatform.domain.model.ChatMessage;
import java.util.List;

/** Streamed LLM completion: tokens are pushed to the sink as they arrive. Blocks until done. */
public interface StreamingCompletionPort {

    void stream(List<ChatMessage> messages, TokenSink sink);
}
