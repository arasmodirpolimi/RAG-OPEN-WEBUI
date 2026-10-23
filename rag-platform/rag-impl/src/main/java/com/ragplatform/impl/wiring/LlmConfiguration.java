package com.ragplatform.impl.wiring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragplatform.impl.anthropic.AnthropicChatAdapter;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.ollama.OllamaChatAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exactly one LLM adapter is active; it serves both CompletionPort and StreamingCompletionPort. */
@Configuration(proxyBeanMethods = false)
public class LlmConfiguration {

    @Bean
    @ConditionalOnProperty(name = "rag.llm.provider", havingValue = "ollama", matchIfMissing = true)
    OllamaChatAdapter ollamaChat(ObjectMapper objectMapper, RagProperties props) {
        return new OllamaChatAdapter(objectMapper, props.ollama().url(), props.ollama().chatModel(),
                props.ollama().timeout());
    }

    @Bean
    @ConditionalOnProperty(name = "rag.llm.provider", havingValue = "anthropic")
    AnthropicChatAdapter anthropicChat(ObjectMapper objectMapper, RagProperties props) {
        RagProperties.Anthropic a = props.anthropic();
        return new AnthropicChatAdapter(objectMapper, a.url(), a.apiKey(), a.model(), a.maxTokens(), a.timeout());
    }
}
