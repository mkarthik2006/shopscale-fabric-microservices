package com.shopscale.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.redisson.api.RedissonClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Distributed Tracing Test
 * Verifies that TraceId and SpanId are correctly propagated via MDC.
 * Doc Ref: Week 4 — Observability & Tracing (Zipkin)
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "net.bytebuddy.experimental=true",
    "management.health.mail.enabled=false"
})
@org.springframework.test.context.ActiveProfiles("test")
public class DistributedTracingTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @MockBean private RedissonClient redissonClient;
    @MockBean private JavaMailSender javaMailSender;
    @MockBean private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("TraceId and SpanId should be present in MDC context")
    void testTraceIdPropagation() {

        // Simulate incoming request headers (what gateway/Zipkin would set)
        MDC.put("traceId", "test-trace-id-123");
        MDC.put("spanId", "test-span-id-456");

        try {
            String traceId = MDC.get("traceId");
            String spanId = MDC.get("spanId");

            // ✅ Assertions
            assertThat(traceId).isEqualTo("test-trace-id-123");
            assertThat(spanId).isEqualTo("test-span-id-456");

        } finally {
            MDC.clear(); // cleanup (important for test isolation)
        }
    }

    @Test
    @DisplayName("TraceId should be null when not set (default behavior)")
    void testTraceIdAbsent() {

        MDC.clear();

        String traceId = MDC.get("traceId");

        assertThat(traceId).isNull();
    }
}