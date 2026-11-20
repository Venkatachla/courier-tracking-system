package com.courier.gateway.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {
    private final ConcurrentHashMap<String, Window> counters = new ConcurrentHashMap<>();
    private final AtomicLong attempts = new AtomicLong();
    private final long windowMillis;
    private final int generalLimit;
    private final int authLimit;

    public InMemoryRateLimiter(
            @Value("${gateway.rate-limit.general-limit:120}") int generalLimit,
            @Value("${gateway.rate-limit.auth-limit:10}") int authLimit,
            @Value("${gateway.rate-limit.window-seconds:60}") long windowSeconds) {
        if (generalLimit < 1 || authLimit < 1 || windowSeconds < 1)
            throw new IllegalArgumentException("Rate limit values must be positive");
        this.generalLimit = generalLimit;
        this.authLimit = authLimit;
        this.windowMillis = windowSeconds * 1000;
    }

    public long consume(String clientIp, boolean authEndpoint) {
        long generalRetry = consumeWindow("general:" + clientIp, generalLimit);
        if (generalRetry > 0) return generalRetry;
        return authEndpoint ? consumeWindow("auth:" + clientIp, authLimit) : 0;
    }

    private long consumeWindow(String key, int limit) {
        long now = System.currentTimeMillis();
        Window window = counters.computeIfAbsent(key, ignored -> new Window(now));
        long retryAfter = window.tryConsume(now, windowMillis, limit);
        if (attempts.incrementAndGet() % 1024 == 0) counters.entrySet().removeIf(entry -> entry.getValue().expired(now, windowMillis));
        return retryAfter;
    }

    private static final class Window {
        private long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
        private synchronized long tryConsume(long now, long duration, int limit) {
            if (now - startedAt >= duration) { startedAt = now; count = 0; }
            if (count >= limit) return Math.max(1, (long) Math.ceil((duration - (now - startedAt)) / 1000.0));
            count++;
            return 0;
        }
        private synchronized boolean expired(long now, long duration) { return now - startedAt >= duration; }
    }
}
