package com.newsagg.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token-bucket parameters. Question (b) requires capacity = 5 tokens and
 * refill = 1 token/second — those are the defaults here (overridable via env).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimiterProperties {

    /** Maximum tokens a single client bucket can hold (burst size). */
    private int capacity = 5;

    /** Tokens added per second (sustained rate). */
    private double refillPerSecond = 1.0;

    /** Redis key prefix for buckets. */
    private String prefix = "ratelimit";

    /** TTL (seconds) per bucket key so stale clients are cleaned up. */
    private long ttlSeconds = 3600;

    /** Backend: "redis" (Lua, atomic) or "memory" (test double). */
    private String store = "redis";
}
