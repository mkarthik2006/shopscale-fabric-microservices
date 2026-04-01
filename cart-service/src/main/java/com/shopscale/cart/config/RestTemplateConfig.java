package com.shopscale.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class RestTemplateConfig {


    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        // Connection Pool Manager
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(100);              // Max total connections
        connectionManager.setDefaultMaxPerRoute(20);     // Per service connections

        // Validate stale connections before use
        connectionManager.setValidateAfterInactivity(Timeout.ofSeconds(5));

        // Fail-Fast Timeout Configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(2))   // Connection timeout
                .setResponseTimeout(Timeout.ofSeconds(4))  // Read timeout
                .build();

        // HTTP Client with Eviction Strategy
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictIdleConnections(Timeout.ofSeconds(30)) // Evict idle connections
                .setDefaultRequestConfig(requestConfig)
                .build();

        // Request Factory
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // Build RestTemplate (Retains Spring Boot auto-config: tracing, metrics)
        return builder
                .requestFactory(() -> factory)
                .additionalInterceptors((request, body, execution) -> {

                    // Optional debug logging (safe for production with DEBUG level)
                    log.debug("Outgoing request: {} {}", request.getMethod(), request.getURI());

                    return execution.execute(request, body);
                })
                .build();
    }
}