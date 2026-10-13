package com.jobportal.ratelimit;

/**
 * Persistent state of a client's bucket as stored in the backend store.
 *
 * @param tokens           current token quantity (fractional allowed)
 * @param lastRefillMillis epoch millis of the last refill write
 */
public record BucketState(double tokens, long lastRefillMillis) {

    public BucketState {
        if (tokens < 0) {
            tokens = 0.0;
        }
    }
}
