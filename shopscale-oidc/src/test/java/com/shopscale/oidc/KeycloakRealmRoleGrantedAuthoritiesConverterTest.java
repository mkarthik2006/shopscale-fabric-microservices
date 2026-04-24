package com.shopscale.oidc;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleGrantedAuthoritiesConverterTest {

    private final KeycloakRealmRoleGrantedAuthoritiesConverter converter =
            new KeycloakRealmRoleGrantedAuthoritiesConverter();

    @Test
    void shouldAvoidDoublePrefixWhenRoleAlreadyContainsRolePrefix() {
        Jwt jwt = buildJwt(List.of("ROLE_ADMIN", "user"));

        Set<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    private Jwt buildJwt(List<String> roles) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("realm_access", Map.of("roles", roles))
        );
    }
}
