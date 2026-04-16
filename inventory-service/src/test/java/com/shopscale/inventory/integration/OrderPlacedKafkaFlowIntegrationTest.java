package com.shopscale.inventory.integration;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.InventoryServiceApplication;
import com.shopscale.inventory.model.InboxEventStatus;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.repository.InboxEventRepository;
import com.shopscale.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Week 2 review criterion: order placed event is consumed from Kafka and inventory is updated.
 */
@SpringBootTest(classes = InventoryServiceApplication.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {"order.placed", "inventory.failure"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderPlacedKafkaFlowIntegrationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @BeforeEach
    void prepare() {
        Mockito.when(jwtDecoder.decode(anyString())).thenAnswer(inv -> {
            String token = inv.getArgument(0);
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("sub", "integration-test")
                    .issuer("http://integration.test")
                    .build();
        });

        inboxEventRepository.deleteAll();
        inventoryRepository.deleteAll();
        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P-IT-1");
        inv.setStock(10);
        inventoryRepository.save(inv);
    }

    @Test
    @DisplayName("Kafka OrderPlacedEvent reduces stock after async consumer processing")
    void kafkaOrderPlaced_reducesInventoryStock() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                eventId,
                "ORDER_PLACED",
                Instant.now(),
                orderId,
                "test-user",
                List.of(new OrderPlacedEvent.Item("P-IT-1", 3, new BigDecimal("9.99"))),
                new BigDecimal("29.97"),
                "USD"
        );

        kafkaTemplate.send("order.placed", orderId.toString(), event).get(10, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryEntity inv = inventoryRepository.findById("P-IT-1").orElseThrow();
            assertThat(inv.getStock()).isEqualTo(7);
            assertThat(inboxEventRepository.findById(eventId)).isPresent();
            assertThat(inboxEventRepository.findById(eventId).orElseThrow().getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        });
    }
}
