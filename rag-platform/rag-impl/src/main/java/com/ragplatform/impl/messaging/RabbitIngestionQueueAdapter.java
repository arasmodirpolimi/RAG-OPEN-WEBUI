package com.ragplatform.impl.messaging;

import com.ragplatform.application.port.out.IngestionQueuePort;
import com.ragplatform.domain.model.DocumentId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** Publishes the document id to the ingest queue via the default exchange (routing key = queue name). */
public final class RabbitIngestionQueueAdapter implements IngestionQueuePort {

    private final RabbitTemplate rabbit;
    private final String queue;

    public RabbitIngestionQueueAdapter(RabbitTemplate rabbit, String queue) {
        this.rabbit = rabbit;
        this.queue = queue;
    }

    @Override
    public void enqueue(DocumentId documentId) {
        rabbit.convertAndSend(queue, documentId.toString());
    }
}
