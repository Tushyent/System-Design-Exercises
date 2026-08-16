package com.ratelimiter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link InMemoryBucketStore} (the exact decision logic also used by
 * the Redis Lua script) including atomicity under concurrency.
 */
class InMemoryBucketStoreTest {

    private static final long T0 = 1_000_000L;
    private InMemoryBucketStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryBucketStore();
    }

    @Test
    void firstSeenClientGetsAFullBucket() {
        Decision d = store.acquire("alice", 10, 5.0, T0);
        assertTrue(d.allowed());
        assertEquals(9.0, d.remainingTokens(), 1e-9);
    }

    @Test
    void burstIsLimitedToCapacityWhenNoRefill() {
        int allowed = 0;
        for (int i = 0; i < 25; i++) {
            if (store.acquire("alice", 10, 0.0, T0 + i).allowed()) {
                allowed++;
            }
        }
        assertEquals(10, allowed, "exactly capacity admits, then rejection");
    }

    @Test
    void clientsAreIsolated() {
        for (int i = 0; i < 20; i++) {
            store.acquire("alice", 10, 0.0, T0);
        }
        assertTrue(store.acquire("bob", 10, 0.0, T0).allowed(), "bob starts with his own full bucket");
    }

    @Test
    void stateReflectsRefillWithoutConsuming() {
        store.acquire("alice", 10, 5.0, T0);
        Optional<BucketState> state = store.state("alice", 10, 5.0, T0 + 2_000);
        assertTrue(state.isPresent());
        assertEquals(10.0, state.get().tokens(), 1e-9, "2s at 5/s adds 10 to the 9 left, capped at capacity 10");
    }

    @Test
    void resetRestoresAFullBucket() {
        for (int i = 0; i < 20; i++) {
            store.acquire("alice", 10, 0.0, T0);
        }
        store.reset("alice");
        assertTrue(store.acquire("alice", 10, 0.0, T0).allowed());
    }

    @Test
    void concurrentAcquiresNeverOverspendTheBucket() throws InterruptedException {
        int threads = 20;
        int perThread = 50;
        int capacity = 5;
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
                    if (store.acquire("shared", capacity, 0.0, T0 + i).allowed()) {
                        allowed.incrementAndGet();
                    }
                }
                done.countDown();
            }).start();
        }
        go.countDown();
        done.await();

        assertEquals(capacity, allowed.get(),
                "with zero refill, concurrent requests may admit at most capacity tokens");
    }
}