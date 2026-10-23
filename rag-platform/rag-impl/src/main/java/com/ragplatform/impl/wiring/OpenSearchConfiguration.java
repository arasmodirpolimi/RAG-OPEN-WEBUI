package com.ragplatform.impl.wiring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragplatform.agent.port.KeywordSearchPort;
import com.ragplatform.agent.port.VectorSearchPort;
import com.ragplatform.application.port.out.ChunkIndexPort;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.http.RestClients;
import com.ragplatform.impl.opensearch.OpenSearchChunkIndexAdapter;
import com.ragplatform.impl.opensearch.OpenSearchGateway;
import com.ragplatform.impl.opensearch.OpenSearchIndexInitializer;
import com.ragplatform.impl.opensearch.OpenSearchKeywordSearchAdapter;
import com.ragplatform.impl.opensearch.OpenSearchVectorSearchAdapter;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenSearchConfiguration {

    @Bean
    OpenSearchGateway openSearchGateway(RagProperties props) {
        return new OpenSearchGateway(RestClients.create(props.opensearch().url(), Duration.ofSeconds(60)));
    }

    @Bean
    OpenSearchIndexInitializer openSearchIndexInitializer(OpenSearchGateway gateway, RagProperties props) {
        return new OpenSearchIndexInitializer(gateway, props.opensearch());
    }

    @Bean
    KeywordSearchPort keywordSearch(OpenSearchGateway gateway, RagProperties props) {
        return new OpenSearchKeywordSearchAdapter(gateway, props.opensearch().indexAlias());
    }

    @Bean
    VectorSearchPort vectorSearch(OpenSearchGateway gateway, RagProperties props) {
        return new OpenSearchVectorSearchAdapter(gateway, props.opensearch().indexAlias());
    }

    @Bean
    ChunkIndexPort chunkIndex(OpenSearchGateway gateway, ObjectMapper objectMapper, RagProperties props) {
        return new OpenSearchChunkIndexAdapter(gateway, objectMapper, props.opensearch().indexAlias());
    }
}
