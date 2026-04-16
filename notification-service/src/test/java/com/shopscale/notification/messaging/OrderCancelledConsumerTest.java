package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.notification.model.InboxEventEntity;
import com.shopscale.notification.model.InboxEventStatus;
import com.shopscale.notification.repository.InboxEventRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancelledConsumerTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private InboxEventRepository inboxEventRepository;
    @InjectMocks
    private OrderCancelledConsumer consumer;

    @Test
    @DisplayName("consume - sends cancellation email and marks inbox as PROCESSED")
    void consumeShouldSendEmailAndMarkProcessed() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                "ORDER_CANCELLED",
                Instant.now(),
                UUID.randomUUID(),
                "user-1",
                "NO_STOCK",
                new BigDecimal("99.99"),
                "USD"
        );
        when(inboxEventRepository.findById(event.eventId())).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any(InboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
        ArgumentCaptor<InboxEventEntity> inboxCaptor = ArgumentCaptor.forClass(InboxEventEntity.class);
        verify(inboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(inboxCaptor.capture());
        assertThat(inboxCaptor.getAllValues().getLast().getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("consume - skips duplicate cancellation events")
    void consumeShouldSkipDuplicateEvent() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                "ORDER_CANCELLED",
                Instant.now(),
                UUID.randomUUID(),
                "user-1",
                "NO_STOCK",
                new BigDecimal("99.99"),
                "USD"
        );
        when(inboxEventRepository.findById(event.eventId()))
                .thenReturn(Optional.of(new InboxEventEntity(event.eventId(), event.eventType(), InboxEventStatus.PROCESSED)));

        consumer.consume(event);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
