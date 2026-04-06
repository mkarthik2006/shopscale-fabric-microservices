package com.shopscale.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * DLQ for Order Service consumers (inventory.failure topic).
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${app.kafka.dlq.max-retries:3}")
    private long maxRetries;

    @Value("${app.kafka.dlq.retry-interval-ms:1000}")
    private long retryIntervalMs;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryIntervalMs, maxRetries)
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Kafka retry attempt {} for topic={}, key={}: {}",
                    deliveryAttempt, record.topic(), record.key(), ex.getMessage());
        });

        return errorHandler;
    }
}