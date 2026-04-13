package com.shopscale.apigateway;

import com.shopscale.apigateway.filter.DistributedMinuteRateLimitGlobalFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(ApiGatewayApplicationTest.RedisTestConfig.class)
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

    @TestConfiguration
    static class RedisTestConfig {
        @Bean
        @Primary
        ReactiveStringRedisTemplate reactiveStringRedisTemplate() {
            @SuppressWarnings("unchecked")
            ReactiveValueOperations<String, String> ops = Mockito.mock(ReactiveValueOperations.class);
            when(ops.increment(anyString())).thenReturn(Mono.just(1L));

            ReactiveStringRedisTemplate tpl = Mockito.mock(ReactiveStringRedisTemplate.class);
            when(tpl.opsForValue()).thenReturn(ops);
            when(tpl.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
            return tpl;
        }
    }

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Autowired
    private DistributedMinuteRateLimitGlobalFilter rateLimitFilter;

    @DynamicPropertySource
    static void disableRemoteInfra(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.gateway.discovery.locator.enabled", () -> "false");
    }

    @BeforeEach
    void stubJwt() {
        when(reactiveJwtDecoder.decode(anyString())).thenAnswer(inv -> {
            String token = inv.getArgument(0);
            Jwt jwt = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .issuer("http://gateway.test")
                    .build();
            return Mono.just(jwt);
        });
    }

    @Test
    void contextLoads_and_rateLimitFilterWired() {
        assertThat(rateLimitFilter).isNotNull();
    }
}
