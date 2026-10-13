package com.library.ratelimit;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class TokenBucketRateLimiter {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> script;

    // In-memory fallback bucket state
    private static class LocalBucket {
        double tokens;
        long lastTimestamp;

        LocalBucket(double capacity) {
            this.tokens = capacity;
            this.lastTimestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();

    // Metrics counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);
    private final AtomicLong totalOverheadNanos = new AtomicLong(0);

    @PostConstruct
    public void init() {
        try {
            script = new DefaultRedisScript<>();
            script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
            script.setResultType(Long.class);
            log.info("Loaded token_bucket.lua successfully");
        } catch (Exception e) {
            log.warn("Could not load token_bucket.lua, fallback to in-memory: {}", e.getMessage());
        }
    }

    public RateLimiterDecision tryAcquire(String key, int capacity, int refillPerSecond) {
        long startNanos = System.nanoTime();
        totalRequests.incrementAndGet();

        RateLimiterDecision decision;
        if (redisTemplate != null && script != null) {
            try {
                decision = acquireRedis(key, capacity, refillPerSecond);
            } catch (Exception e) {
                log.warn("Redis rate limiter failed for key {}, falling back to in-memory: {}", key, e.getMessage());
                decision = acquireLocal(key, capacity, refillPerSecond);
            }
        } else {
            decision = acquireLocal(key, capacity, refillPerSecond);
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        decision.setExecutionTimeNanos(elapsedNanos);
        totalOverheadNanos.addAndGet(elapsedNanos);

        if (decision.isAllowed()) {
            allowedRequests.incrementAndGet();
        } else {
            rejectedRequests.incrementAndGet();
        }

        return decision;
    }

    private RateLimiterDecision acquireRedis(String key, int capacity, int refillPerSecond) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis();
        long ttlSeconds = Math.max(60, (long) Math.ceil((double) capacity / refillPerSecond) * 2);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(redisKey),
                String.valueOf(capacity),
                String.valueOf(refillPerSecond),
                String.valueOf(now),
                String.valueOf(ttlSeconds)
        );

        if (result == null) {
            return RateLimiterDecision.builder().allowed(true).remainingTokens(capacity).build();
        }

        if (result >= 0) {
            double remaining = result / 1000.0;
            return RateLimiterDecision.builder()
                    .allowed(true)
                    .remainingTokens(remaining)
                    .retryAfterSeconds(0)
                    .build();
        } else {
            double remaining = (-result - 1) / 1000.0;
            long retryAfter = (long) Math.ceil((1.0 - remaining) / refillPerSecond);
            if (retryAfter < 1) retryAfter = 1;
            return RateLimiterDecision.builder()
                    .allowed(false)
                    .remainingTokens(remaining)
                    .retryAfterSeconds(retryAfter)
                    .build();
        }
    }

    private RateLimiterDecision acquireLocal(String key, int capacity, int refillPerSecond) {
        long now = System.currentTimeMillis();
        LocalBucket bucket = localBuckets.computeIfAbsent(key, k -> new LocalBucket(capacity));

        synchronized (bucket) {
            double elapsedSeconds = (now - bucket.lastTimestamp) / 1000.0;
            bucket.tokens = Math.min(capacity, bucket.tokens + elapsedSeconds * refillPerSecond);
            bucket.lastTimestamp = now;

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return RateLimiterDecision.builder()
                        .allowed(true)
                        .remainingTokens(bucket.tokens)
                        .retryAfterSeconds(0)
                        .build();
            } else {
                long retryAfter = (long) Math.ceil((1.0 - bucket.tokens) / refillPerSecond);
                if (retryAfter < 1) retryAfter = 1;
                return RateLimiterDecision.builder()
                        .allowed(false)
                        .remainingTokens(bucket.tokens)
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
        }
    }

    public Map<String, Object> getMetrics() {
        long total = totalRequests.get();
        long allowed = allowedRequests.get();
        long rejected = rejectedRequests.get();
        double rejectionRate = total > 0 ? ((double) rejected / total) * 100.0 : 0.0;
        double avgOverheadMs = total > 0 ? (totalOverheadNanos.get() / (double) total) / 1_000_000.0 : 0.0;

        return Map.of(
                "totalRequests", total,
                "allowedRequests", allowed,
                "rejectedRequests", rejected,
                "rejectionRatePercent", rejectionRate,
                "avgOverheadMs", avgOverheadMs
        );
    }

    public void resetMetrics() {
        totalRequests.set(0);
        allowedRequests.set(0);
        rejectedRequests.set(0);
        totalOverheadNanos.set(0);
        localBuckets.clear();
    }
}
