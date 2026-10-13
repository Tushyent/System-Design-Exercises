package com.jobportal.ratelimit;

import java.util.Optional;

/**
 * Abstraction over the rate-limit state store. The production implementation
 * ({@link RedisBucketStore}) performs an atomic read-modify-write with a Redis
 * Lua script; the in-memory implementation is a test double with identical semantics.
 */
public interface BucketStore {

    /**
     * Atomically attempt to consume one token for {@code clientId}.
     *
     * @param capacity        maximum tokens the bucket can hold
     * @param tokensPerSecond refill rate (tokens added per second)
     * @param nowMillis       current time in epoch millis
     */
    Decision acquire(String clientId, int capacity, double tokensPerSecond, long nowMillis);

    /**
     * Read the current bucket state WITHOUT consuming a token. An empty result
     * means the client has never been seen; its bucket is implicitly full.
     */
    Optional<BucketState> state(String clientId, int capacity, double tokensPerSecond, long nowMillis);

    /** Drop the bucket for one client (used by reset / demo replay). */
    void reset(String clientId);

    /** Drop every bucket (used by test isolation). */
    void resetAll();
}
