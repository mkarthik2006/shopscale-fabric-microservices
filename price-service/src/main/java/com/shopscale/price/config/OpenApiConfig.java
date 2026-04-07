package com.shopscale.price.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI priceServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Price Service API")
                        .description("Real-time pricing engine with Redis caching")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ShopScale Engineering")
                                .email("engineering@shopscale.dev")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Direct"),
                        new Server().url("http://localhost:9080").description("Via Gateway")
                ));
    }
}