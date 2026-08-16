package com.ratelimiter.ratelimit;

/**
 * Read-only view of a client's bucket (no tokens consumed).
 *
 * @param clientId          the identified client
 * @param currentTokens     tokens currently available (after applying refill)
 * @param capacity          bucket capacity
 * @param refillPerSecond   configured refill rate
 * @param lastRefillMillis  last state-write time (epoch millis)
 * @param retryAfterSeconds estimated seconds before one token is available, 0 when ready
 */
public record BucketStatus(String clientId,
                           double currentTokens,
                           int capacity,
                           double refillPerSecond,
                           long lastRefillMillis,
                           long retryAfterSeconds) {
}