package com.shopscale.oidc;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class OAuth2ResourceServerSupport {

    private OAuth2ResourceServerSupport() {}

    public static JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleGrantedAuthoritiesConverter());
        return converter;
    }
}
