package com.shopscale.cart.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void cartServiceOpenAPI_shouldContainExpectedMetadata() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "serverPort", "8086");

        OpenAPI openAPI = config.cartServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Cart Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("engineering@shopscale.dev");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("http://localhost:8086");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("http://localhost:9080");
    }
}
