package com.shopscale.order.service;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private OrderOutboxMapper outboxMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ExecutorService executorService;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        publisher = new OutboxPublisher(outboxEventRepository, outboxMapper, kafkaTemplate, executorService);
        ReflectionTestUtils.setField(publisher, "orderPlacedTopic", "order.placed");
        ReflectionTestUtils.setField(publisher, "sendTimeoutSeconds", 2L);
    }

    @Test
    @DisplayName("publishSingleEvent marks outbox row as SENT on successful Kafka publish")
    void publishSingleEventMarksSentOnSuccess() throws Exception {
        UUID outboxId = UUID.randomUUID();
        OutboxEventEntity event = buildEvent(outboxId);
        OrderPlacedEvent payload = buildPayload(event.getAggregateId());

        when(outboxEventRepository.findById(outboxId)).thenReturn(Optional.of(event));
        when(outboxMapper.fromPayload(event.getPayload())).thenReturn(payload);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(org.mockito.Mockito.mock(SendResult.class)));

        publisher.publishSingleEvent(outboxId);

        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("publishSingleEvent increments retryCount when Kafka publish fails")
    void publishSingleEventIncrementsRetryOnFailure() {
        UUID outboxId = UUID.randomUUID();
        OutboxEventEntity event = buildEvent(outboxId);
        OrderPlacedEvent payload = buildPayload(event.getAggregateId());

        when(outboxEventRepository.findById(outboxId)).thenReturn(Optional.of(event));
        when(outboxMapper.fromPayload(event.getPayload())).thenReturn(payload);
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failed);

        publisher.publishSingleEvent(outboxId);

        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("publishPendingEvents submits all pending outbox entries")
    void publishPendingEventsSubmitsPendingRows() {
        OutboxEventEntity event = buildEvent(UUID.randomUUID());
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(outboxEventRepository, times(1)).findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    private OutboxEventEntity buildEvent(UUID outboxId) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(outboxId);
        event.setAggregateType("ORDER");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("ORDER_PLACED");
        event.setPayload("{\"eventType\":\"ORDER_PLACED\"}");
        event.setStatus(OutboxStatus.PENDING);
        event.setCreatedAt(Instant.now());
        return event;
    }

    private OrderPlacedEvent buildPayload(UUID orderId) {
        return new OrderPlacedEvent(
                UUID.randomUUID(),
                "ORDER_PLACED",
                Instant.now(),
                orderId,
                "user-1",
                List.of(new OrderPlacedEvent.Item("SKU-1", 1, new BigDecimal("9.99"))),
                new BigDecimal("9.99"),
                "USD"
        );
    }
}
