package com.shopscale.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void productServiceOpenAPI_shouldBuildExpectedMetadataAndServers() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "serverPort", "8088");

        OpenAPI openAPI = config.productServiceOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Product Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("engineering@shopscale.dev");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("http://localhost:8088");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("http://localhost:9080");
    }
}
