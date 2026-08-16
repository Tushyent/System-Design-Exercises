package com.ratelimiter.ratelimit;

import com.ratelimiter.config.RateLimiterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Production {@link BucketStore} that keeps the per-client token count and last
 * refill timestamp in Redis as a hash {@code ratelimit:{clientId}} = {tokens, ts}.
 *
 * <p>Every acquire is a single {@code EVAL} of {@code scripts/token_bucket.lua}.
 * Because Redis executes Lua scripts atomically and each script reads + refills +
 * consumes + writes in one shot, the same bucket can be mutated concurrently by
 * many application instances without losing tokens (no TOCTOU race). This is what
 * makes the rate limit correct in a horizontally-scaled deployment.
 */
@RequiredArgsConstructor
public class RedisBucketStore implements BucketStore {

    private static final Collection<Object> FIELDS = List.of("tokens", "ts");

    private final StringRedisTemplate redis;
    private final RateLimiterProperties props;
    private final DefaultRedisScript<Long> script;

    @Override
    public Decision acquire(String clientId, int capacity, double tokensPerSecond, long nowMillis) {
        Long result = redis.execute(script,
                Collections.singletonList(key(clientId)),
                String.valueOf(capacity),
                String.valueOf(tokensPerSecond),
                String.valueOf(nowMillis),
                String.valueOf(props.getTtlSeconds()));
        if (result == null) {
            throw new IllegalStateException("Redis returned no result for bucket " + clientId);
        }
        return decode(result, nowMillis);
    }

    @Override
    public Optional<BucketState> state(String clientId, int capacity, double tokensPerSecond, long nowMillis) {
        List<Object> values = redis.opsForHash().multiGet(key(clientId), FIELDS);
        if (values == null || values.get(0) == null || values.get(1) == null) {
            return Optional.empty();
        }
        double tokens = Double.parseDouble(values.get(0).toString());
        long ts = Long.parseLong(values.get(1).toString());
        double refilled = TokenBucketMath.refill(tokens, ts, nowMillis, capacity, tokensPerSecond);
        return Optional.of(new BucketState(refilled, nowMillis));
    }

    @Override
    public void reset(String clientId) {
        redis.delete(key(clientId));
    }

    @Override
    public void resetAll() {
        var keys = redis.keys(props.getPrefix() + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private String key(String clientId) {
        return props.getPrefix() + ":" + clientId;
    }

    private static Decision decode(long encoded, long nowMillis) {
        boolean allowed = encoded >= 0;
        double remaining;
        if (allowed) {
            remaining = encoded / 1000.0;
        } else {
            remaining = (-encoded - 1) / 1000.0;
        }
        return new Decision(allowed, remaining, nowMillis);
    }
}