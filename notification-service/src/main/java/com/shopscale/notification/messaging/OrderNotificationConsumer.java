package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.notification.model.ProcessedNotificationEntity;
import com.shopscale.notification.repository.ProcessedNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification Consumer with Idempotency Guard (Project Doc Page 6).
 * Prevents duplicate emails when Kafka redelivers the same event.
 */
@Component
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final JavaMailSender mailSender;
    private final ProcessedNotificationRepository processedRepo;

    @Value("${app.notification.from:noreply@shopscale.dev}")
    private String fromAddress;

    public OrderNotificationConsumer(JavaMailSender mailSender,
                                      ProcessedNotificationRepository processedRepo) {
        this.mailSender = mailSender;
        this.processedRepo = processedRepo;
    }

    @Transactional
    @KafkaListener(topics = "order.placed", groupId = "notification-order-placed")
    public void consume(OrderPlacedEvent event) {

        // IDEMPOTENCY GUARD: Skip if already processed
        if (processedRepo.existsById(event.eventId())) {
            log.info("Duplicate notification event skipped: {}", event.eventId());
            return;
        }

        log.info("Received order event: {}", event.orderId());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo("customer@example.com");
            message.setSubject("Order Confirmation: " + event.orderId());
            message.setText(
                    "Your order has been placed successfully.\n\n" +
                    "Order ID: " + event.orderId() + "\n" +
                    "Amount: " + event.totalAmount() + " " + event.currency()
            );
            mailSender.send(message);

            // Mark as processed AFTER successful send
            processedRepo.save(new ProcessedNotificationEntity(event.eventId(), "ORDER_PLACED"));
            log.info("Email sent successfully for order {}", event.orderId());

        } catch (Exception e) {
            log.error("Failed to send email for order {}", event.orderId(), e);
            // Don't mark as processed — allow retry on next delivery
        }
    }
}