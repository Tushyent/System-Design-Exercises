package com.ratelimiter.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketMathTest {

    private static final long T0 = 1_000_000L;

    @Test
    void refillAddsTokensAtRateUpToCapacity() {
        double r = TokenBucketMath.refill(5.0, T0, T0 + 2_000, 10, 2.0);
        assertEquals(9.0, r, 1e-9, "2 seconds at 2 tokens/s adds 4 tokens to 5");
    }

    @Test
    void refillNeverExceedsCapacity() {
        double r = TokenBucketMath.refill(9.5, T0, T0 + 10_000, 10, 2.0);
        assertEquals(10.0, r, 1e-9, "long idle time caps at capacity");
    }

    @Test
    void refillIgnoresNegativeElapsed() {
        double r = TokenBucketMath.refill(4.0, T0 + 5_000, T0, 10, 2.0);
        assertEquals(4.0, r, 1e-9, "clock moved backwards -> no refill");
    }

    @Test
    void firstRequestConsumesOneFromFullBucket() {
        Decision d = TokenBucketMath.acquire(10.0, -1, T0, 10, 5.0);
        assertTrue(d.allowed());
        assertEquals(9.0, d.remainingTokens(), 1e-9);
    }

    @Test
    void burstConsumesExactlyCapacityTokens() {
        double tokens = 10.0;
        long now = T0;
        int allowed = 0;
        for (int i = 0; i < 15; i++) {
            Decision d = TokenBucketMath.acquire(tokens, now, now, 10, 0.0);
            if (d.allowed()) {
                allowed++;
                tokens = d.remainingTokens();
            }
        }
        assertEquals(10, allowed, "zero refill -> exactly capacity admits in a burst");
    }

    @Test
    void rejectsWhenEmptyAndRemainingStaysBelowOne() {
        Decision d = TokenBucketMath.acquire(0.3, T0, T0 + 100, 10, 0.0);
        assertFalse(d.allowed());
        assertEquals(0.3, d.remainingTokens(), 1e-9, "rejected request must not consume");
    }

    @Test
    void refillRestoresTokensAfterWait() {
        double tokens = 1.0;
        Decision d1 = TokenBucketMath.acquire(tokens, T0, T0, 10, 5.0);
        assertTrue(d1.allowed());
        assertEquals(0.0, d1.remainingTokens(), 1e-9);

        long now = T0 + 500; // 0.5s at 5/s = +2.5 tokens
        Decision d2 = TokenBucketMath.acquire(d1.remainingTokens(), T0, now, 10, 5.0);
        assertTrue(d2.allowed());
        assertEquals(1.5, d2.remainingTokens(), 1e-9);
    }
}