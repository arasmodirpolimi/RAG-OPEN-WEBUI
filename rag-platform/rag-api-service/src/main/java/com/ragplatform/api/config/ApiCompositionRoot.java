package com.ragplatform.api.config;

import com.ragplatform.agent.answer.AnswerGenerationAgent;
import com.ragplatform.agent.answer.PromptBuilder;
import com.ragplatform.agent.conversation.QueryRewritingAgent;
import com.ragplatform.agent.port.CompletionPort;
import com.ragplatform.agent.port.EmbeddingPort;
import com.ragplatform.agent.port.KeywordSearchPort;
import com.ragplatform.agent.port.RerankPort;
import com.ragplatform.agent.port.StreamingCompletionPort;
import com.ragplatform.agent.port.VectorSearchPort;
import com.ragplatform.agent.retrieval.HybridRetrievalAgent;
import com.ragplatform.agent.retrieval.RetrievalSettings;
import com.ragplatform.agent.retrieval.RrfFusion;
import com.ragplatform.application.port.in.UnifiedChatUseCase;
import com.ragplatform.application.port.in.QueryDocumentsUseCase;
import com.ragplatform.application.port.in.ReindexDocumentUseCase;
import com.ragplatform.application.port.in.UploadDocumentUseCase;
import com.ragplatform.application.port.out.DocumentRepositoryPort;
import com.ragplatform.application.port.out.FileStoragePort;
import com.ragplatform.application.port.out.IngestionQueuePort;
import com.ragplatform.application.service.CollectionPolicy;
import com.ragplatform.application.service.UnifiedChatService;
import com.ragplatform.application.service.QueryDocumentsService;
import com.ragplatform.application.service.ReindexDocumentService;
import com.ragplatform.application.service.UploadDocumentService;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.opensearch.OpenSearchIndexInitializer;
import com.ragplatform.impl.wiring.EmbeddingConfiguration;
import com.ragplatform.impl.wiring.LlmConfiguration;
import com.ragplatform.impl.wiring.MessagingConfiguration;
import com.ragplatform.impl.wiring.OpenSearchConfiguration;
import com.ragplatform.impl.wiring.PersistenceConfiguration;
import com.ragplatform.impl.wiring.RerankerConfiguration;
import java.time.Clock;
import java.util.concurrent.Executors;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Composition root of the query service: the only place where concrete adapters are bound to ports.
 * Domain, Agent and Application classes have no Spring annotations and are created here.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RagProperties.class)
@Import({PersistenceConfiguration.class, OpenSearchConfiguration.class, EmbeddingConfiguration.class,
        LlmConfiguration.class, RerankerConfiguration.class, MessagingConfiguration.class})
public class ApiCompositionRoot {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CollectionPolicy collectionPolicy(RagProperties props) {
        return new CollectionPolicy(props.collections());
    }

    // ---- Agent layer
    @Bean
    HybridRetrievalAgent hybridRetrievalAgent(EmbeddingPort embeddings, KeywordSearchPort keywordSearch,
                                              VectorSearchPort vectorSearch, RerankPort reranker,
                                              RagProperties props) {
        RagProperties.Retrieval r = props.retrieval();
        RetrievalSettings settings = new RetrievalSettings(r.candidatesPerRetriever(), r.rerankCandidates(),
                r.finalTopK(), props.reranker().enabled(), props.reranker().minScore());
        return new HybridRetrievalAgent(embeddings, keywordSearch, vectorSearch, reranker,
                // Not exposed as a bean: an Executor bean would make Boot back off its own task executor.
                new RrfFusion(r.rrfK()), settings, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    QueryRewritingAgent queryRewritingAgent(CompletionPort completion) {
        return new QueryRewritingAgent(completion);
    }

    @Bean
    AnswerGenerationAgent answerGenerationAgent(StreamingCompletionPort streaming) {
        return new AnswerGenerationAgent(streaming, new PromptBuilder());
    }

    // ---- Application layer (use cases)
    @Bean
    UnifiedChatUseCase unifiedChatUseCase(QueryRewritingAgent rewriter, HybridRetrievalAgent retriever,
                                           AnswerGenerationAgent generator, StreamingCompletionPort streaming,
                                           CollectionPolicy collections, RagProperties props) {
        return new UnifiedChatService(rewriter, retriever, generator, streaming, collections,
                props.retrieval().finalTopK());
    }

    @Bean
    UploadDocumentUseCase uploadDocumentUseCase(DocumentRepositoryPort repository, FileStoragePort storage,
                                                IngestionQueuePort queue, CollectionPolicy collections,
                                                Clock clock) {
        return new UploadDocumentService(repository, storage, queue, collections, clock);
    }

    @Bean
    ReindexDocumentUseCase reindexDocumentUseCase(DocumentRepositoryPort repository, IngestionQueuePort queue) {
        return new ReindexDocumentService(repository, queue);
    }

    @Bean
    QueryDocumentsUseCase queryDocumentsUseCase(DocumentRepositoryPort repository, CollectionPolicy collections) {
        return new QueryDocumentsService(repository, collections);
    }

    @Bean
    ApplicationRunner ensureSearchIndex(OpenSearchIndexInitializer initializer) {
        return args -> initializer.ensureIndex();
    }
}
