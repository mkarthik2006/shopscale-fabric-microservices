package com.shopscale.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@Component
public class ClientIpResolver {

    private final List<String> trustedProxyIps;

    public ClientIpResolver(@Value("${app.rate-limit.trusted-proxies:}") String trustedProxies) {
        this.trustedProxyIps = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String resolveClientIp(ServerHttpRequest request) {
        String remoteIp = resolveRemoteIp(request);
        if (isTrustedProxy(remoteIp)) {
            String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String firstHop = forwarded.split(",")[0].trim();
                if (isValidIp(firstHop)) {
                    return firstHop;
                }
            }
        }
        return remoteIp;
    }

    private boolean isTrustedProxy(String remoteIp) {
        if (!isValidIp(remoteIp)) {
            return false;
        }
        return trustedProxyIps.contains(remoteIp);
    }

    private boolean isValidIp(String value) {
        try {
            InetAddress.getByName(value);
            return true;
        } catch (java.net.UnknownHostException ex) {
            return false;
        }
    }

    private String resolveRemoteIp(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return "unknown";
    }
}
