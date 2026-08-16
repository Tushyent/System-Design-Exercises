package com.ratelimiter.ratelimit;

/**
 * Pure token-bucket decision logic. This is the reference implementation that the
 * Redis Lua script mirrors; keeping the math in one tested Java class avoids
 * drift between the two and makes the algorithm unit-testable without Redis.
 */
public final class TokenBucketMath {

    private TokenBucketMath() {
    }

    /**
     * Refill a token level up to {@code capacity} based on the time elapsed since
     * the last refill. Tokens are added at {@code tokensPerSecond}.
     */
    public static double refill(double tokens, long lastRefillMillis, long nowMillis,
                                int capacity, double tokensPerSecond) {
        if (nowMillis <= lastRefillMillis || lastRefillMillis < 0) {
            return Math.min(capacity, tokens);
        }
        double elapsedSeconds = (nowMillis - lastRefillMillis) / 1000.0;
        return Math.min(capacity, tokens + elapsedSeconds * tokensPerSecond);
    }

    /**
     * Attempt to consume exactly one token.
     *
     * @return a {@link Decision} whose {@code allowed} flag is {@code true} when a
     * token was available (and consumed), or {@code false} otherwise.
     */
    public static Decision acquire(double tokens, long lastRefillMillis, long nowMillis,
                                   int capacity, double tokensPerSecond) {
        double refilled = refill(tokens, lastRefillMillis, nowMillis, capacity, tokensPerSecond);
        boolean allowed = refilled >= 1.0;
        double remaining = allowed ? refilled - 1.0 : refilled;
        return new Decision(allowed, remaining, nowMillis);
    }
}