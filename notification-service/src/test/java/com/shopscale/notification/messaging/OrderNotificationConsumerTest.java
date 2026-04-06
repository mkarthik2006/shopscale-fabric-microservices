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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderNotificationConsumerTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private OrderNotificationConsumer consumer;

    @Test
    @DisplayName("consume - sends confirmation email on OrderPlacedEvent")
    void consume_shouldSendEmail() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(), "ORDER_PLACED", Instant.now(),
                orderId, "U-001",
                List.of(new OrderPlacedEvent.Item("P1", 2, new BigDecimal("199.99"))),
                new BigDecimal("399.98"), "USD"
        );

        consumer.consume(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getSubject()).contains(orderId.toString());
        assertThat(msg.getText()).contains("399.98");
        assertThat(msg.getText()).contains("USD");
    }

    @Test
    @DisplayName("consume - does not throw when mail sender fails")
    void consume_shouldHandleMailFailureGracefully() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(), "ORDER_PLACED", Instant.now(),
                UUID.randomUUID(), "U-001",
                List.of(new OrderPlacedEvent.Item("P1", 1, new BigDecimal("14.99"))),
                new BigDecimal("14.99"), "USD"
        );

        // Should NOT throw — logs error instead
        consumer.consume(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}