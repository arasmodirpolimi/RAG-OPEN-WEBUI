package com.ragplatform.impl.wiring;

import com.ragplatform.agent.port.RerankPort;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.http.RestClients;
import com.ragplatform.impl.reranker.TeiRerankAdapter;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RerankerConfiguration {

    @Bean
    RerankPort rerankPort(RagProperties props) {
        return new TeiRerankAdapter(RestClients.create(props.reranker().url(), Duration.ofSeconds(60)));
    }
}
