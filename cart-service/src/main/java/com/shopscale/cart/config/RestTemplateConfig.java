package com.shopscale.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {


        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(2))
                .setValidateAfterInactivity(Timeout.ofSeconds(5))
                .build();


        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setDefaultConnectionConfig(connectionConfig);


        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(4))
                .build();


        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictIdleConnections(Timeout.ofSeconds(30))
                .setDefaultRequestConfig(requestConfig)
                .build();


        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);


        return builder
                .requestFactory(() -> factory)
                .additionalInterceptors((request, body, execution) -> {
                    log.debug("Outgoing request: {} {}", request.getMethod(), request.getURI());
                    ServletRequestAttributes attrs =
                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attrs != null) {
                        String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                        if (auth != null && !auth.isBlank()) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}