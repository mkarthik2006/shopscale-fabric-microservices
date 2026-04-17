package com.shopscale.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Value("${app.security.cors.allowed-origin-patterns:http://localhost:3000}")
    private String allowedOriginPatterns;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headers -> headers
                        .xssProtection(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                )

                // ✅ CORS (KEEP)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeExchange(ex -> ex

                        // ✅ HEALTH + INFO
                        .pathMatchers("/actuator/health/**", "/actuator/info/**").permitAll()

                        // ✅ FALLBACK (REQUIRED for CircuitBreaker)
                        .pathMatchers("/fallback/**").permitAll()

                        // ✅ KEYCLOAK AUTH FLOW
                        .pathMatchers("/auth/**").permitAll()

                        // ✅ ADMIN SECURITY
                        .pathMatchers("/admin/**").hasRole("ADMIN")

                        // ✅ ACTUATOR (ADMIN ONLY)
                        .pathMatchers("/actuator/**").hasRole("ADMIN")

                        // ✅ CORS PREFLIGHT
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ PUBLIC PRODUCT API (IMPORTANT)
                        .pathMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()

                        // ✅ SECURED BUSINESS APIs
                        .pathMatchers(
                                "/api/orders", "/api/orders/**",
                                "/api/inventory", "/api/inventory/**",
                                "/api/cart", "/api/cart/**",
                                "/api/prices", "/api/prices/**"
                        ).authenticated()

                        // ✅ DEFAULT RULE
                        .anyExchange().authenticated()
                )

                // ✅ JWT (Keycloak integration)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                .build();
    }

    // ✅ CORS CONFIG (KEEP)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> patterns = new ArrayList<>();
        for (String origin : allowedOriginPatterns.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                patterns.add(trimmed);
            }
        }
        config.setAllowedOriginPatterns(patterns.isEmpty() ? Collections.singletonList("http://localhost:3000") : patterns);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "x-requested-with"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}