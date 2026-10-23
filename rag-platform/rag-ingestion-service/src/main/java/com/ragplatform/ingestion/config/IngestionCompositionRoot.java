package com.ragplatform.ingestion.config;

import com.ragplatform.agent.chunking.ChunkingSettings;
import com.ragplatform.agent.chunking.DocumentChunker;
import com.ragplatform.agent.chunking.MarkdownStructureChunker;
import com.ragplatform.agent.chunking.RepeatedLineFilter;
import com.ragplatform.agent.chunking.WordBasedTokenEstimator;
import com.ragplatform.agent.port.EmbeddingPort;
import com.ragplatform.application.port.in.ProcessDocumentUseCase;
import com.ragplatform.application.port.out.ChunkIndexPort;
import com.ragplatform.application.port.out.DocumentParserPort;
import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.application.port.out.FileStoragePort;
import com.ragplatform.application.service.ProcessDocumentService;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.wiring.DoclingConfiguration;
import com.ragplatform.impl.wiring.EmbeddingConfiguration;
import com.ragplatform.impl.wiring.MessagingConfiguration;
import com.ragplatform.impl.wiring.OpenSearchConfiguration;
import com.ragplatform.impl.wiring.PersistenceConfiguration;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Composition root of the ingestion worker. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RagProperties.class)
@Import({PersistenceConfiguration.class, OpenSearchConfiguration.class, EmbeddingConfiguration.class,
        DoclingConfiguration.class, MessagingConfiguration.class})
public class IngestionCompositionRoot {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    DocumentChunker documentChunker(RagProperties props) {
        return new MarkdownStructureChunker(
                new ChunkingSettings(props.chunking().maxTokens(), props.chunking().overlapTokens()),
                new WordBasedTokenEstimator(), new RepeatedLineFilter());
    }

    @Bean
    ProcessDocumentUseCase processDocumentUseCase(DocumentRepositoryPort repository, FileStoragePort storage,
                                                  DocumentParserPort parser, DocumentChunker chunker,
                                                  EmbeddingPort embeddings, ChunkIndexPort index, Clock clock,
                                                  RagProperties props) {
        return new ProcessDocumentService(repository, storage, parser, chunker, embeddings, index, clock,
                props.chunking().embeddingBatchSize());
    }
}
