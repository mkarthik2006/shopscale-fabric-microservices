package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.notification.model.InboxEventEntity;
import com.shopscale.notification.model.InboxEventStatus;
import com.shopscale.notification.repository.InboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification Consumer with Idempotency Guard (Project Doc Page 6).
 * Prevents duplicate emails when Kafka redelivers the same event.
 */
@Component
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final JavaMailSender mailSender;
    private final InboxEventRepository inboxEventRepository;
    private final ObjectProvider<OrderNotificationConsumer> selfProvider;

    @Value("${app.notification.from:noreply@shopscale.dev}")
    private String fromAddress = "noreply@shopscale.dev";

    public OrderNotificationConsumer(JavaMailSender mailSender,
                                     InboxEventRepository inboxEventRepository,
                                     ObjectProvider<OrderNotificationConsumer> selfProvider) {
        this.mailSender = mailSender;
        this.inboxEventRepository = inboxEventRepository;
        this.selfProvider = selfProvider;
    }

    @KafkaListener(topics = "${app.kafka.topic.order-placed:order.placed}", groupId = "notification-order-placed")
    public void consume(OrderPlacedEvent event) {
        OrderNotificationConsumer self = selfProvider.getObject();
        InboxEventEntity inbox = self.claimInbox(event);
        if (inbox == null) {
            log.info("Duplicate notification event skipped: {}", event.eventId());
            return;
        }
        log.info("Received order event: {}", event.orderId());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(resolveRecipient(event.userEmail()));
            message.setSubject("Order Confirmation: " + event.orderId());
            message.setText(
                    "Your order has been placed successfully.\n\n" +
                    "Order ID: " + event.orderId() + "\n" +
                    "Amount: " + event.totalAmount() + " " + event.currency()
            );
            mailSender.send(message);
            self.markProcessed(event.eventId());
            log.info("Email sent successfully for order {}", event.orderId());

        } catch (Exception e) {
            log.error("Failed to send email for order {}", event.orderId(), e);
            // Re-throw so Kafka error handler can apply retry / DLT policy.
            throw new RuntimeException("Notification send failed for order " + event.orderId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InboxEventEntity claimInbox(OrderPlacedEvent event) {
        var existing = inboxEventRepository.findById(event.eventId());
        if (existing.isPresent() && existing.get().getStatus() == InboxEventStatus.PROCESSED) {
            return null;
        }
        InboxEventEntity inbox = existing.orElseGet(() ->
                inboxEventRepository.save(new InboxEventEntity(event.eventId(), event.eventType(), InboxEventStatus.RECEIVED))
        );
        if (inbox.getStatus() != InboxEventStatus.RECEIVED) {
            return null;
        }
        int claimed = inboxEventRepository.updateStatusIfCurrent(event.eventId(), InboxEventStatus.RECEIVED, InboxEventStatus.IN_PROGRESS);
        if (claimed == 0) {
            return null;
        }
        inbox.setStatus(InboxEventStatus.IN_PROGRESS);
        return inbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(java.util.UUID eventId) {
        InboxEventEntity inbox = inboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Inbox event not found: " + eventId));
        inbox.setStatus(InboxEventStatus.PROCESSED);
        inbox.setProcessedAt(java.time.Instant.now());
        inboxEventRepository.save(inbox);
    }

    private String resolveRecipient(String userEmail) {
        if (userEmail != null && !userEmail.isBlank() && userEmail.contains("@")) {
            return userEmail;
        }
        throw new IllegalArgumentException("Recipient email missing in order placed event");
    }
}