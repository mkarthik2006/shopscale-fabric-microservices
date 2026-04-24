package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.notification.model.InboxEventEntity;
import com.shopscale.notification.model.InboxEventStatus;
import com.shopscale.notification.repository.InboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for OrderNotificationConsumer (Notification Service)
 * Doc Ref: Week 2 — "Notification Service emails user"
 * Doc Ref: Page 3 — "MailHog for email testing"
 * Doc Ref: Page 6 — Resilience principle
 */
@ExtendWith(MockitoExtension.class)
class OrderNotificationConsumerTest {

    @Mock private JavaMailSender mailSender;
    @Mock private InboxEventRepository inboxEventRepository;
    @Mock private ObjectProvider<OrderNotificationConsumer> selfProvider;

    private OrderPlacedEvent createEvent(UUID orderId, BigDecimal amount, String currency) {
        return new OrderPlacedEvent(
                UUID.randomUUID(), "ORDER_PLACED", Instant.now(),
                orderId, "U-001", "user001@shopscale.dev",
                List.of(new OrderPlacedEvent.Item("P1", 2, new BigDecimal("199.99"))),
                amount, currency
        );
    }

    private OrderNotificationConsumer createConsumer() {
        OrderNotificationConsumer consumer = new OrderNotificationConsumer(mailSender, inboxEventRepository, selfProvider);
        when(selfProvider.getObject()).thenReturn(consumer);
        return consumer;
    }

    private void stubInboxHappyPath(OrderPlacedEvent event) {
        InboxEventEntity received = new InboxEventEntity(event.eventId(), event.eventType(), InboxEventStatus.RECEIVED);
        when(inboxEventRepository.findById(event.eventId()))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(received));
        when(inboxEventRepository.updateStatusIfCurrent(
                eq(event.eventId()), eq(InboxEventStatus.RECEIVED), eq(InboxEventStatus.IN_PROGRESS)))
                .thenReturn(1);
        when(inboxEventRepository.save(any(InboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("consume — sends confirmation email with correct orderId in subject")
    void consume_shouldSendEmailWithOrderId() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = createEvent(orderId, new BigDecimal("399.98"), "USD");

        // Stub idempotency check
        stubInboxHappyPath(event);

        OrderNotificationConsumer consumer = createConsumer();
        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getSubject()).isEqualTo("Order Confirmation: " + orderId);
    }

    @Test
    @DisplayName("consume — email body contains totalAmount and currency")
    void consume_shouldIncludeAmountAndCurrencyInBody() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = createEvent(orderId, new BigDecimal("399.98"), "USD");

        stubInboxHappyPath(event);

        OrderNotificationConsumer consumer = createConsumer();
        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("399.98");
        assertThat(captor.getValue().getText()).contains("USD");
    }

    @Test
    @DisplayName("consume — email sent to resolved user recipient")
    void consume_shouldSendToCorrectRecipient() {
        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("14.99"), "USD");

        stubInboxHappyPath(event);

        OrderNotificationConsumer consumer = createConsumer();
        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getTo()).containsExactly("user001@shopscale.dev");
    }

    @Test
    @DisplayName("consume — email body contains orderId")
    void consume_shouldIncludeOrderIdInBody() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = createEvent(orderId, new BigDecimal("199.99"), "USD");

        stubInboxHappyPath(event);

        OrderNotificationConsumer consumer = createConsumer();
        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains(orderId.toString());
    }

    @Test
    @DisplayName("consume — rethrows on mail sender failure for Kafka retry/DLT")
    void consume_shouldRethrowWhenMailSenderFails() {
        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("14.99"), "USD");

        stubInboxHappyPath(event);
        doThrow(new RuntimeException("SMTP connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        OrderNotificationConsumer consumer = createConsumer();
        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notification send failed");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("consume — handles different currency values")
    void consume_shouldHandleDifferentCurrencies() {
        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("1500.00"), "EUR");

        stubInboxHappyPath(event);

        OrderNotificationConsumer consumer = createConsumer();
        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("1500.00");
        assertThat(captor.getValue().getText()).contains("EUR");
    }

    @Test
    @DisplayName("consume — skips duplicate events (idempotency guard)")
    void consume_shouldSkipDuplicateEvent() {
        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("199.99"), "USD");

        // Already processed
        when(inboxEventRepository.findById(event.eventId()))
                .thenReturn(java.util.Optional.of(new InboxEventEntity(event.eventId(), event.eventType(), InboxEventStatus.PROCESSED)));

        OrderNotificationConsumer consumer = createConsumer();
        consumer.consume(event);

        // No email sent, no save
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(inboxEventRepository, never()).save(any());
    }
}