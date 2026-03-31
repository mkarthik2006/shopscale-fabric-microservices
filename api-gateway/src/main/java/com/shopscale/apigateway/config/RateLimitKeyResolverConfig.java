package com.shopscale.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitKeyResolverConfig {

  @Bean
  public KeyResolver userOrIpKeyResolver() {
    return exchange -> {
      String auth = exchange.getRequest().getHeaders().getFirst("Authorization");

      if (auth != null && !auth.isBlank()) {
        return Mono.just(auth);
      }

      String ip = exchange.getRequest().getRemoteAddress() != null
          ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
          : "unknown";

      return Mono.just(ip);
    };
  }
}