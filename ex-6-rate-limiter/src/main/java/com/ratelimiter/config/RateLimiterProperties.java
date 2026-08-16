package com.ratelimiter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable token-bucket parameters. Overridable via environment variables
 * ({@code RATELIMIT_CAPACITY}, {@code RATELIMIT_REFILL_PER_SECOND}, ...).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimiterProperties {

    /** Maximum tokens a single client bucket can hold. */
    private int capacity = 10;

    /** Number of tokens added to the bucket per second. */
    private double refillPerSecond = 5.0;

    /** Redis key prefix for rate-limit buckets. */
    private String prefix = "ratelimit";

    /** TTL (seconds) applied to each bucket key so stale clients are cleaned up. */
    private long ttlSeconds = 3600;

    /**
     * Backend store: {@code redis} (default, Lua-script atomic) or
     * {@code memory} (test double).
     */
    private String store = "redis";
}