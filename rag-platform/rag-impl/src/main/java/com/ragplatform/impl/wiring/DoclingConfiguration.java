package com.ragplatform.impl.wiring;

import com.ragplatform.application.port.out.DocumentParserPort;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.docling.DoclingParserAdapter;
import com.ragplatform.impl.http.RestClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DoclingConfiguration {

    @Bean
    DocumentParserPort documentParser(RagProperties props) {
        return new DoclingParserAdapter(
                RestClients.create(props.docling().url(), props.docling().timeout()),
                props.docling().ocrLanguages());
    }
}
