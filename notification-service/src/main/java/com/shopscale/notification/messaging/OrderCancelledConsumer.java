package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.notification.model.InboxEventEntity;
import com.shopscale.notification.model.InboxEventStatus;
import com.shopscale.notification.repository.InboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SAGA Notification Consumer (Project Doc Page 6).
 * Sends cancellation email when an order fails the inventory check.
 */
@Component
public class OrderCancelledConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledConsumer.class);

    private final JavaMailSender mailSender;
    private final InboxEventRepository inboxEventRepository;

    @Value("${app.notification.from:noreply@shopscale.dev}")
    private String fromAddress;

    public OrderCancelledConsumer(JavaMailSender mailSender, InboxEventRepository inboxEventRepository) {
        this.mailSender = mailSender;
        this.inboxEventRepository = inboxEventRepository;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.topic.order-cancelled:order.cancelled}", groupId = "notification-order-cancelled")
    public void consume(OrderCancelledEvent event) {
        var existing = inboxEventRepository.findById(event.eventId());
        if (existing.isPresent() && existing.get().getStatus() == InboxEventStatus.PROCESSED) {
            log.info("Duplicate cancellation notification skipped: {}", event.eventId());
            return;
        }
        InboxEventEntity inbox = existing.orElseGet(() ->
                inboxEventRepository.save(new InboxEventEntity(event.eventId(), event.eventType(), InboxEventStatus.RECEIVED))
        );

        log.info("Received cancellation event for order: {}", event.orderId());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo("customer@example.com");
            message.setSubject("Order Cancelled: " + event.orderId());
            message.setText(
                "Your order has been cancelled due to inventory issues.\n\n" +
                "Order ID: " + event.orderId() + "\n" +
                "Reason: " + event.reason() + "\n" +
                "Refund Amount: " + event.totalAmount() + " " + event.currency() + "\n\n" +
                "We apologize for the inconvenience."
            );
            mailSender.send(message);
            inbox.setStatus(InboxEventStatus.PROCESSED);
            inbox.setProcessedAt(java.time.Instant.now());
            inboxEventRepository.save(inbox);
            log.info("Cancellation email sent for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to send cancellation email for order {}", event.orderId(), e);
        }
    }
}