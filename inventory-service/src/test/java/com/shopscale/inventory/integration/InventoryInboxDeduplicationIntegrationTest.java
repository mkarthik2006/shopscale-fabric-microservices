package com.shopscale.inventory.integration;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.InboxEventStatus;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.repository.InboxEventRepository;
import com.shopscale.inventory.repository.InventoryRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("resource")
class InventoryInboxDeduplicationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("inventorydb")
            .withUsername("shopscale")
            .withPassword("shopscale");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Test
    @DisplayName("Duplicate OrderPlacedEvent is processed once due to inbox deduplication")
    void duplicateOrderPlacedEventIsIgnored() {
        InventoryEntity inv = new InventoryEntity();
        inv.setSku("SKU-DEDUP-1");
        inv.setStock(10);
        inventoryRepository.save(inv);

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                eventId,
                "ORDER_PLACED",
                Instant.now(),
                orderId,
                "user-dedup",
                "user-dedup@shopscale.dev",
                List.of(new OrderPlacedEvent.Item("SKU-DEDUP-1", 2, new BigDecimal("10.00"))),
                new BigDecimal("20.00"),
                "USD"
        );

        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<String, Object> producer = new KafkaTemplate<>(producerFactory);

        producer.send("order.placed", orderId.toString(), event);
        producer.send("order.placed", orderId.toString(), event);
        producer.flush();

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            InventoryEntity updated = inventoryRepository.findById("SKU-DEDUP-1").orElseThrow();
            assertThat(updated.getStock()).isEqualTo(8);
            assertThat(inboxEventRepository.findById(eventId)).isPresent();
            assertThat(inboxEventRepository.findById(eventId).orElseThrow().getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        });
    }
}
