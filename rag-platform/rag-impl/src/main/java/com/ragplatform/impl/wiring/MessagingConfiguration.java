package com.ragplatform.impl.wiring;

import com.ragplatform.application.port.out.IngestionQueuePort;
import com.ragplatform.impl.config.RagProperties;
import com.ragplatform.impl.messaging.RabbitIngestionQueueAdapter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue topology: messages rejected after the listener's retries are dead-lettered (default exchange)
 * to the dead-letter queue, where they can be inspected in the RabbitMQ UI.
 */
@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

    @Bean
    Queue ingestQueue(RagProperties props) {
        return QueueBuilder.durable(props.queue().ingest())
                .deadLetterExchange("")
                .deadLetterRoutingKey(props.queue().deadLetter())
                .build();
    }

    @Bean
    Queue ingestDeadLetterQueue(RagProperties props) {
        return QueueBuilder.durable(props.queue().deadLetter()).build();
    }

    @Bean
    IngestionQueuePort ingestionQueue(RabbitTemplate rabbitTemplate, RagProperties props) {
        return new RabbitIngestionQueueAdapter(rabbitTemplate, props.queue().ingest());
    }
}
