package com.shopscale.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("native")
class ConfigServerApplicationTest {

    @DynamicPropertySource
    static void configRepoLocation(DynamicPropertyRegistry registry) {
        Path repo = Path.of("").toAbsolutePath().getParent().resolve("config-repo");
        registry.add("spring.cloud.config.server.native.search-locations", () -> "file:" + repo);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Test
    void contextLoads() {
    }
}
