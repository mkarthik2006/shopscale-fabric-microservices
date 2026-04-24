package com.shopscale.order.integration;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.repository.OutboxEventRepository;
import com.shopscale.order.service.OrderService;
import com.shopscale.order.service.OutboxPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "app.outbox.publisher.fixed-delay-ms=60000"
})
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("resource")
class OrderOutboxIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orderdb")
            .withUsername("shopscale")
            .withPassword("shopscale");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private static KafkaConsumer<String, String> testConsumer;

    @BeforeAll
    static void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-outbox-it-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        testConsumer = new KafkaConsumer<>(props);
        testConsumer.subscribe(Collections.singletonList("order.placed"));
    }

    @AfterAll
    static void tearDownConsumer() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("Order placement writes outbox row and publisher emits to Kafka")
    void orderPlacementWritesOutboxAndPublishesEvent() {
        OrderEntity order = new OrderEntity();
        order.setUserId("integration-user");
        order.setUserEmail("integration-user@shopscale.dev");
        order.setCurrency("USD");
        order.setTotalAmount(new BigDecimal("19.99"));

        OrderItemEmbeddable item = new OrderItemEmbeddable();
        item.setSku("SKU-INT-1");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("19.99"));
        order.setItems(java.util.List.of(item));

        OrderEntity saved = orderService.placeOrder(order);
        assertThat(saved.getId()).isNotNull();

        assertThat(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).hasSize(1);
        outboxPublisher.publishPendingEvents();

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).isEmpty()
        );

        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
        boolean matchingOrderRecord = false;
        for (ConsumerRecord<String, String> record : records) {
            if (record.key() != null && record.key().equals(saved.getId().toString())) {
                matchingOrderRecord = true;
                break;
            }
        }
        assertThat(matchingOrderRecord).isTrue();
    }
}
