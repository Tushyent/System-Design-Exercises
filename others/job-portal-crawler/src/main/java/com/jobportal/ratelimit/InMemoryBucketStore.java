package com.jobportal.ratelimit;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory {@link BucketStore} used for unit tests and the
 * {@code ratelimit.store=memory} profile. {@code ConcurrentHashMap.compute}
 * gives per-key atomicity equivalent to the Lua script.
 */
public class InMemoryBucketStore implements BucketStore {

    private final ConcurrentMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    @Override
    public Decision acquire(String clientId, int capacity, double tokensPerSecond, long nowMillis) {
        AtomicReference<Decision> decision = new AtomicReference<>();
        buckets.compute(clientId, (key, state) -> {
            double tokens = state == null ? capacity : state.tokens();
            long lastRefill = state == null ? -1 : state.lastRefillMillis();
            Decision d = TokenBucketMath.acquire(tokens, lastRefill, nowMillis, capacity, tokensPerSecond);
            decision.set(d);
            return new BucketState(d.remainingTokens(), nowMillis);
        });
        return decision.get();
    }

    @Override
    public Optional<BucketState> state(String clientId, int capacity, double tokensPerSecond, long nowMillis) {
        BucketState state = buckets.get(clientId);
        if (state == null) {
            return Optional.empty();
        }
        double refilled = TokenBucketMath.refill(state.tokens(), state.lastRefillMillis(), nowMillis,
                capacity, tokensPerSecond);
        return Optional.of(new BucketState(refilled, nowMillis));
    }

    @Override
    public void reset(String clientId) {
        buckets.remove(clientId);
    }

    @Override
    public void resetAll() {
        buckets.clear();
    }
}
