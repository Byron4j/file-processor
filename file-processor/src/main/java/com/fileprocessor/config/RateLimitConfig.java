package com.fileprocessor.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // Store rate limiters per user/IP
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @Bean
    public Map<String, RateLimiter> rateLimiters() {
        return rateLimiters;
    }

    public RateLimiter createNewLimiter() {
        // 100 requests per second
        return RateLimiter.create(100.0);
    }

    public RateLimiter createStrictLimiter() {
        // 10 requests per second
        return RateLimiter.create(10.0);
    }

    public RateLimiter getLimiter(String key) {
        return rateLimiters.computeIfAbsent(key, k -> createNewLimiter());
    }
}
