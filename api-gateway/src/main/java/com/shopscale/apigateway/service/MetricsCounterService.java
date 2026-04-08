package com.shopscale.apigateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricsCounterService {

    private final StringRedisTemplate redisTemplate;
    private static final String TRAFFIC_COUNTER_KEY = "system:metrics:total_requests";

    public MetricsCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * ✅ STRICT RULE: Distributed Counters
     * Can be invoked by a gateway filter to track total system traffic distribution.
     */
    public Long incrementTrafficCounter() {
        return redisTemplate.opsForValue().increment(TRAFFIC_COUNTER_KEY);
    }
}
