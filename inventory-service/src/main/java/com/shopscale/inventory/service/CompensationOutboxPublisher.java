package com.shopscale.inventory.service;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.inventory.model.CompensationOutboxEntity;
import com.shopscale.inventory.model.CompensationOutboxStatus;
import com.shopscale.inventory.repository.CompensationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CompensationOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(CompensationOutboxPublisher.class);

    private final CompensationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectProvider<CompensationOutboxPublisher> selfProvider;

    @Value("${app.kafka.topic.inventory-failure:inventory.failure}")
    private String failureTopic;

    @Value("${app.outbox.publisher.send-timeout-seconds:5}")
    private long sendTimeoutSeconds;

    public CompensationOutboxPublisher(CompensationOutboxRepository outboxRepository,
                                       KafkaTemplate<String, Object> kafkaTemplate,
                                       ObjectProvider<CompensationOutboxPublisher> selfProvider) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.selfProvider = selfProvider;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    @Transactional(readOnly = true)
    public void publishPendingEvents() {
        List<CompensationOutboxEntity> pending =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(CompensationOutboxStatus.PENDING);
        CompensationOutboxPublisher self = selfProvider.getObject();
        for (CompensationOutboxEntity event : pending) {
            self.publishSingleEvent(event.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishSingleEvent(java.util.UUID outboxId) {
        int claimed = outboxRepository.updateStatusIfCurrent(
                outboxId, CompensationOutboxStatus.PENDING, CompensationOutboxStatus.IN_PROGRESS);
        if (claimed == 0) {
            return;
        }

        CompensationOutboxEntity event = outboxRepository.findById(outboxId).orElse(null);
        if (event == null || event.getStatus() != CompensationOutboxStatus.IN_PROGRESS) {
            return;
        }

        InventoryInsufficientEvent payload = new InventoryInsufficientEvent(
                event.getOrderId(),
                event.getSourceEventId(),
                event.getSku(),
                event.getReason()
        );
        try {
            kafkaTemplate.send(failureTopic, event.getOrderId().toString(), payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            event.setStatus(CompensationOutboxStatus.SENT);
            event.setSentAt(Instant.now());
            outboxRepository.save(event);
            log.info("Compensation outbox event published | outboxId={} orderId={}", event.getId(), event.getOrderId());
        } catch (Exception ex) {
            event.setStatus(CompensationOutboxStatus.PENDING);
            outboxRepository.save(event);
            log.warn("Compensation outbox publish failed; will retry | outboxId={} orderId={}",
                    event.getId(), event.getOrderId(), ex);
        }
    }
}
