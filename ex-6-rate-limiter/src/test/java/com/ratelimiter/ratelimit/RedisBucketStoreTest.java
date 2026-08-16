package com.ratelimiter.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the real Redis + Lua path. These tests only run when a
 * Redis server is reachable on {@code spring.data.redis.host:port}; otherwise
 * they are skipped so that {@code mvn test} still passes without Docker.
 */
@SpringBootTest(properties = {
        "ratelimit.store=redis",
        "ratelimit.capacity=10",
        "ratelimit.refill-per-second=5",
        "ratelimit.prefix=ratelimit:it"
})
class RedisBucketStoreTest {

    @Autowired
    private BucketStore store;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void assumeRedisIsUpAndClean() {
        Assumptions.assumeTrue(redisAvailable(), "Redis not reachable; skipping integration test");
        safeResetAll();
    }

    @AfterEach
    void cleanup() {
        safeResetAll();
    }

    private void safeResetAll() {
        try {
            store.resetAll();
        } catch (RuntimeException ignored) {
            // Redis may already be gone; this is best-effort cleanup only.
        }
    }

    private boolean redisAvailable() {
        try {
            return redis.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void firstSeenClientStartsWithFullBucket() {
        Decision d = store.acquire("lua-alice", 10, 5.0, 1_000_000L);
        assertTrue(d.allowed());
        assertEquals(9.0, d.remainingTokens(), 1e-9);
    }

    @Test
    void burstLimitedToCapacityWhenNoRefill() {
        int allowed = 0;
        for (int i = 0; i < 25; i++) {
            if (store.acquire("lua-burst", 10, 0.0, 1_000_000L).allowed()) {
                allowed++;
            }
        }
        assertEquals(10, allowed, "Lua script must cap a zero-refill burst at capacity");
    }

    @Test
    void rejectedRequestDoesNotConsume() {
        for (int i = 0; i < 12; i++) {
            store.acquire("lua-strict", 10, 0.0, 1_000_000L);
        }
        Decision rejected = store.acquire("lua-strict", 10, 0.0, 1_000_000L);
        assertFalse(rejected.allowed());
        assertEquals(0.0, rejected.remainingTokens(), 1e-9);
    }

    @Test
    void tokensRefillOverTime() throws InterruptedException {
        for (int i = 0; i < 15; i++) {
            store.acquire("lua-refill", 10, 0.0, System.currentTimeMillis());
        }
        Thread.sleep(1100);
        Decision d = store.acquire("lua-refill", 10, 5.0, System.currentTimeMillis());
        assertTrue(d.allowed(), "after ~1.1s at 5 tokens/s the bucket must have >= 1 token");
    }

    @Test
    void stateDoesNotConsume() {
        store.acquire("lua-state", 10, 5.0, 1_000_000L);
        var before = store.state("lua-state", 10, 5.0, 1_000_000L);
        var after = store.state("lua-state", 10, 5.0, 1_000_000L);
        assertTrue(before.isPresent());
        assertEquals(before.get().tokens(), after.get().tokens(), 1e-9, "state() must not consume tokens");
    }

    @Test
    void resetRestoresFullBucket() {
        for (int i = 0; i < 15; i++) {
            store.acquire("lua-reset", 10, 0.0, 1_000_000L);
        }
        assertFalse(store.acquire("lua-reset", 10, 0.0, 1_000_000L).allowed());
        store.reset("lua-reset");
        assertTrue(store.acquire("lua-reset", 10, 0.0, 1_000_000L).allowed());
    }

    @Test
    void concurrentLuaAcquiresNeverOverspend() throws InterruptedException {
        int threads = 20;
        int perThread = 100;
        int capacity = 7;
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicLong allowed = new AtomicLong();

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < perThread; i++) {
                    if (store.acquire("lua-load", capacity, 0.0, System.currentTimeMillis()).allowed()) {
                        allowed.incrementAndGet();
                    }
                }
                done.countDown();
            }).start();
        }
        go.countDown();
        done.await();

        assertEquals(capacity, allowed.get(),
                "2000 concurrent requests with zero refill must admit exactly capacity tokens");
    }
}