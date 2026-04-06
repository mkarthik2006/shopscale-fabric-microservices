package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for OrderNotificationConsumer (Notification Service)
 * Doc Ref: Week 2 — "Notification Service emails user"
 * Doc Ref: Page 3 — "MailHog for email testing"
 * Doc Ref: Page 6 — "If the Review Service fails, the Checkout Service must remain fully operational" (resilience principle)
 */
@ExtendWith(MockitoExtension.class)
class OrderNotificationConsumerTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private OrderNotificationConsumer consumer;

    private OrderPlacedEvent createEvent(UUID orderId, BigDecimal amount, String currency) {
        return new OrderPlacedEvent(
                UUID.randomUUID(), "ORDER_PLACED", Instant.now(),
                orderId, "U-001",
                List.of(new OrderPlacedEvent.Item("P1", 2, new BigDecimal("199.99"))),
                amount, currency
        );
    }

    @Test
    @DisplayName("consume — sends confirmation email with correct orderId in subject")
    void consume_shouldSendEmailWithOrderId() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = createEvent(orderId, new BigDecimal("399.98"), "USD");

        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        // OrderNotificationConsumer.java L33: "Order Confirmation: " + event.orderId()
        assertThat(msg.getSubject()).isEqualTo("Order Confirmation: " + orderId);
    }

    @Test
    @DisplayName("consume — email body contains totalAmount and currency")
    void consume_shouldIncludeAmountAndCurrencyInBody() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = createEvent(orderId, new BigDecimal("399.98"), "USD");

        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        // OrderNotificationConsumer.java L37: "Amount: " + event.totalAmount() + " " + event.currency()
        assertThat(captor.getValue().getText()).contains("399.98");
        assertThat(captor.getValue().getText()).contains("USD");
    }

    @Test
    @DisplayName("consume — email sent to customer@example.com")
    void consume_shouldSendToCorrectRecipient() {
        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("14.99"), "USD");

        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        // OrderNotificationConsumer.java L31: message.setTo("customer@example.com")
        assertThat(captor.getValue().getTo()).containsExactly("customer@example.com");
    }

    @Test
    @DisplayName("consume — email body contains orderId")
    void consume_shouldIncludeOrderIdInBody() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = createEvent(orderId, new BigDecimal("199.99"), "USD");

        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        // OrderNotificationConsumer.java L36: "Order ID: " + event.orderId()
        assertThat(captor.getValue().getText()).contains(orderId.toString());
    }

    @Test
    @DisplayName("consume — does NOT throw when mail sender fails (graceful degradation)")
    void consume_shouldHandleMailFailureGracefully() {
        // OrderNotificationConsumer.java L44-46: catch (Exception e) { log.error(...) }
        doThrow(new RuntimeException("SMTP connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("14.99"), "USD");

        // Must NOT throw — consumer logs error and continues
        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("consume — handles different currency values")
    void consume_shouldHandleDifferentCurrencies() {
        OrderPlacedEvent event = createEvent(UUID.randomUUID(), new BigDecimal("1500.00"), "EUR");

        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("1500.00");
        assertThat(captor.getValue().getText()).contains("EUR");
    }
}