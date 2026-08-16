package com.ratelimiter.ratelimit;

/**
 * Result of trying to acquire one token from a client's bucket.
 *
 * @param allowed          {@code true} if the request was permitted (one token consumed)
 * @param remainingTokens  quantity of tokens left in the bucket after the decision
 * @param updatedAtMillis  the instant (epoch millis) the bucket state was last written
 */
public record Decision(boolean allowed, double remainingTokens, long updatedAtMillis) {

    public Decision {
        if (remainingTokens < 0) {
            remainingTokens = 0.0;
        }
    }
}