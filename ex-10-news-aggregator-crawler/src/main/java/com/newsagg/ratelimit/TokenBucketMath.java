package com.newsagg.ratelimit;

/**
 * Pure token-bucket mathematics shared by both stores so the algorithm can be
 * verified without a running Redis.
 */
public final class TokenBucketMath {

    private TokenBucketMath() {
    }

    /** Tokens available after applying refill since {@code lastRefillMillis}. */
    public static double refill(double tokens, long lastRefillMillis, long nowMillis,
                                int capacity, double tokensPerSecond) {
        if (lastRefillMillis < 0) {
            return capacity; // never seen: bucket starts full
        }
        double elapsedSeconds = Math.max(0, nowMillis - lastRefillMillis) / 1000.0;
        return Math.min(capacity, tokens + elapsedSeconds * tokensPerSecond);
    }

    /** Apply refill then consume one token when available. */
    public static Decision acquire(double tokens, long lastRefillMillis, long nowMillis,
                                   int capacity, double tokensPerSecond) {
        double refilled = refill(tokens, lastRefillMillis, nowMillis, capacity, tokensPerSecond);
        if (refilled >= 1.0) {
            return new Decision(true, refilled - 1.0, nowMillis);
        }
        return new Decision(false, refilled, nowMillis);
    }
}
