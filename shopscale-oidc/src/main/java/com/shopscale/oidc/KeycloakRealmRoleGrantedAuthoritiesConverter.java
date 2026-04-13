package com.shopscale.oidc;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class KeycloakRealmRoleGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }
        Object raw = realmAccess.get("roles");
        if (!(raw instanceof Collection<?> roles)) {
            return Collections.emptyList();
        }
        List<GrantedAuthority> out = new ArrayList<>();
        for (Object r : roles) {
            if (r != null) {
                out.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
            }
        }
        return out;
    }
}
