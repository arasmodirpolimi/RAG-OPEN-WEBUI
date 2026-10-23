package com.ragplatform.ingestion.messaging;

import com.ragplatform.application.port.in.ProcessDocumentUseCase;
import com.ragplatform.domain.model.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter: one message = one document id. Exceptions trigger the configured retries; after the
 * last attempt the message is rejected without requeue and dead-lettered (see rag-common.yml / topology).
 */
@Component
public class IngestionListener {

    private static final Logger LOG = LoggerFactory.getLogger(IngestionListener.class);

    private final ProcessDocumentUseCase process;

    public IngestionListener(ProcessDocumentUseCase process) {
        this.process = process;
    }

    @RabbitListener(queues = "${rag.queue.ingest}")
    public void onMessage(String documentId) {
        LOG.info("Processing document {}", documentId);
        long start = System.nanoTime();
        process.process(DocumentId.of(documentId));
        LOG.info("Indexed document {} in {} ms", documentId, (System.nanoTime() - start) / 1_000_000);
    }
}
