package com.shopscale.order.service;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderOutboxMapper outboxMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ExecutorService executorService;

    @Value("${app.kafka.topic.order-placed:order.placed}")
    private String orderPlacedTopic;

    @Value("${app.outbox.publisher.send-timeout-seconds:5}")
    private long sendTimeoutSeconds;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           OrderOutboxMapper outboxMapper,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ExecutorService executorService) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxMapper = outboxMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.executorService = executorService;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pending) {
            executorService.submit(() -> publishSingleEvent(event.getId()));
        }
    }

    @Transactional
    public void publishSingleEvent(java.util.UUID outboxId) {
        OutboxEventEntity event = outboxEventRepository.findById(outboxId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        try {
            OrderPlacedEvent payload = outboxMapper.fromPayload(event.getPayload());
            kafkaTemplate.send(orderPlacedTopic, event.getAggregateId().toString(), payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
            log.info("Outbox event published successfully | outboxId={} orderId={}", event.getId(), event.getAggregateId());
        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(ex.getMessage());
            outboxEventRepository.save(event);
            log.warn("Outbox publish failed (will retry) | outboxId={} retryCount={}",
                    event.getId(), event.getRetryCount(), ex);
        }
    }
}
