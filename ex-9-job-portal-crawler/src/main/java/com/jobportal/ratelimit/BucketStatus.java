package com.jobportal.ratelimit;

/**
 * Read-only view of a client's bucket (no token consumed).
 */
public record BucketStatus(String clientId,
                           double currentTokens,
                           int capacity,
                           double refillPerSecond,
                           long lastRefillMillis,
                           long retryAfterSeconds) {
}
