package com.ragplatform.impl.wiring;

import com.ragplatform.agent.port.EmbeddingPort;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.http.RestClients;
import com.ragplatform.impl.ollama.OllamaEmbeddingAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EmbeddingConfiguration {

    @Bean
    EmbeddingPort embeddingPort(RagProperties props) {
        return new OllamaEmbeddingAdapter(
                RestClients.create(props.ollama().url(), props.ollama().timeout()),
                props.ollama().embeddingModel(),
                props.opensearch().dimension());
    }
}
