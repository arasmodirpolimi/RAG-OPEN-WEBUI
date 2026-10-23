package com.ragplatform.impl.wiring;

import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.application.port.out.FileStoragePort;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.persistence.JdbcDocumentRepositoryAdapter;
import com.ragplatform.impl.storage.LocalFileStorageAdapter;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
public class PersistenceConfiguration {

    @Bean
    DocumentRepositoryPort documentRepository(JdbcClient jdbcClient) {
        return new JdbcDocumentRepositoryAdapter(jdbcClient);
    }

    @Bean
    FileStoragePort fileStorage(RagProperties props) {
        return new LocalFileStorageAdapter(Path.of(props.storage().root()));
    }
}
