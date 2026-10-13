package com.library.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
    @DisplayName("Should allow requests up to capacity and reject excess burst requests")
    void testRateLimitingBurstAndRejection() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        limiter.resetMetrics();

        int capacity = 5;
        int refillPerSec = 1;
        String clientKey = "test-client-1";

        // First 5 requests should all be allowed
        for (int i = 0; i < capacity; i++) {
            RateLimiterDecision decision = limiter.tryAcquire(clientKey, capacity, refillPerSec);
            assertTrue(decision.isAllowed(), "Request " + (i + 1) + " should be allowed within capacity");
        }

        // 6th request immediately afterwards should be rejected
        RateLimiterDecision rejected = limiter.tryAcquire(clientKey, capacity, refillPerSec);
        assertFalse(rejected.isAllowed(), "6th request should be rejected exceeding capacity");
        assertTrue(rejected.getRetryAfterSeconds() >= 1, "Retry-After should be at least 1 second");
    }
}
