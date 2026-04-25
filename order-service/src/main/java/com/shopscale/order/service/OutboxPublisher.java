package com.shopscale.order.service;

import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderOutboxMapper outboxMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ExecutorService executorService;



    private final ObjectProvider<OutboxPublisher> selfProvider;

    @Value("${app.kafka.topic.order-placed:order.placed}")
    private String orderPlacedTopic;

    @Value("${app.kafka.topic.order-cancelled:order.cancelled}")
    private String orderCancelledTopic;

    @Value("${app.outbox.publisher.send-timeout-seconds:5}")
    private long sendTimeoutSeconds;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           OrderOutboxMapper outboxMapper,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ExecutorService executorService,
                           ObjectProvider<OutboxPublisher> selfProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxMapper = outboxMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.executorService = executorService;
        this.selfProvider = selfProvider;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    @Transactional(readOnly = true)
    public void publishPendingEvents() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }

        OutboxPublisher self = selfProvider.getObject();
        for (OutboxEventEntity event : pending) {
            java.util.UUID id = event.getId();
            executorService.submit(() -> self.publishSingleEvent(id));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishSingleEvent(java.util.UUID outboxId) {
        int claimed = outboxEventRepository.updateStatusIfCurrent(
                outboxId, OutboxStatus.PENDING, OutboxStatus.IN_PROGRESS
        );
        if (claimed == 0) {
            return;
        }
        OutboxEventEntity event = outboxEventRepository.findById(outboxId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.IN_PROGRESS) {
            return;
        }

        try {
            Object payload = mapPayloadByEventType(event);
            String topic = resolveTopicByEventType(event.getEventType());
            kafkaTemplate.send(topic, event.getAggregateId().toString(), payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
            log.info("Outbox event published successfully | outboxId={} orderId={}", event.getId(), event.getAggregateId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError("Interrupted: " + ex.getMessage());
            outboxEventRepository.save(event);
            log.warn("Outbox publish interrupted (will retry) | outboxId={} retryCount={}",
                    event.getId(), event.getRetryCount(), ex);
        } catch (ExecutionException | TimeoutException ex) {
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            outboxEventRepository.save(event);
            log.warn("Outbox transient publish failure (will retry) | outboxId={} retryCount={}",
                    event.getId(), event.getRetryCount(), ex);
        } catch (IllegalStateException ex) {
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError("Payload mapping failed: " + ex.getMessage());
            outboxEventRepository.save(event);
            log.error("Outbox payload mapping failure (will retry) | outboxId={} retryCount={}",
                    event.getId(), event.getRetryCount(), ex);
        }
    }

    private Object mapPayloadByEventType(OutboxEventEntity event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" -> outboxMapper.fromPayload(event.getPayload());
            case "ORDER_CANCELLED" -> outboxMapper.fromCancelledPayload(event.getPayload());
            default -> throw new IllegalStateException("Unsupported outbox event type: " + event.getEventType());
        };
    }

    private String resolveTopicByEventType(String eventType) {
        return switch (eventType) {
            case "ORDER_PLACED" -> orderPlacedTopic;
            case "ORDER_CANCELLED" -> orderCancelledTopic;
            default -> throw new IllegalStateException("No Kafka topic mapped for outbox event type: " + eventType);
        };
    }
}
