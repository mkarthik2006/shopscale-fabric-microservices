package com.shopscale.price.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void priceServiceOpenAPI_shouldContainExpectedMetadataAndServers() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "serverPort", "8085");

        OpenAPI openAPI = config.priceServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Price Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("engineering@shopscale.dev");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("http://localhost:8085");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("http://localhost:9080");
    }
}
