package com.shopscale.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeExchange(ex -> ex

        // ✅ HEALTH + INFO (PUBLIC)
        .pathMatchers("/actuator/health/**", "/actuator/info/**").permitAll()

        // ✅ STRICT ADMIN PROTECTION (MANDATORY RULE)
        .pathMatchers("/admin/**").hasRole("ADMIN")

        // ✅ ACTUATOR FULL ACCESS ONLY ADMIN
        .pathMatchers("/actuator/**").hasRole("ADMIN")

        // ✅ CORS PREFLIGHT
        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

        // ✅ PUBLIC PRODUCT APIs
        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()

        // ✅ AUTHENTICATED BUSINESS APIs
        .pathMatchers("/api/orders/**", "/api/inventory/**", "/api/cart/**").authenticated()

        // ✅ DEFAULT SECURITY
        .anyExchange().authenticated()
)

                // ENTERPRISE JWT VALIDATION (Auto-configured from YML)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                .build();
    }

    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Collections.singletonList("*")); // Secure this in Production (e.g. your-domain.com)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "x-requested-with"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1-hour pre-flight cache for performance

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
