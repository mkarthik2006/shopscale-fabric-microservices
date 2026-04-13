package com.shopscale.inventory.config;

import com.shopscale.oidc.OAuth2ResourceServerSupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Kafka-only service today; HTTP surface is actuator + future REST. JWT validates inbound calls from the gateway.
 */
@Configuration
@EnableWebSecurity
public class InventoryServiceSecurityConfig {

    @Bean
    SecurityFilterChain inventoryServiceSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(OAuth2ResourceServerSupport.keycloakJwtAuthenticationConverter())));
        return http.build();
    }
}
