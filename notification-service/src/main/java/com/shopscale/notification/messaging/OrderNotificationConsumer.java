package com.shopscale.notification.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final JavaMailSender mailSender;

    public OrderNotificationConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @KafkaListener(topics = "order.placed", groupId = "notification-group")
    public void consume(OrderPlacedEvent event) {

        log.info("Received order event: {}", event.orderId());

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // TODO: Replace with event.getCustomerEmail() when available
            message.setTo("customer@example.com");

            message.setSubject("Order Confirmation: " + event.orderId());
            message.setText(
                    "Your order has been placed successfully.\n\n" +
                    "Order ID: " + event.orderId() + "\n" +
                    "Amount: " + event.totalAmount() + " " + event.currency()
            );

            mailSender.send(message);

            log.info("Email sent successfully for order {}", event.orderId());

        } catch (Exception e) {
            log.error("Failed to send email for order {}", event.orderId(), e);

            // Future improvement:
            // publish to notification.failure topic
        }
    }
}