package com.shopscale.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Explicitly enables JPA repository scanning to prevent Spring Data Redis
 * (pulled in via redisson-spring-boot-starter) from ambiguously claiming
 * JPA repositories — satisfies Cloud-Native config mandate (PROJECT_RULES §1).
 */
@SpringBootApplication(scanBasePackages = {"com.shopscale.notification", "com.shopscale.common"})
@EnableJpaRepositories(basePackages = "com.shopscale.notification.repository")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}