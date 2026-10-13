package com.library.idgenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("Should generate positive, monotonically increasing IDs")
    void testBasicGeneration() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        long id1 = generator.nextId();
        long id2 = generator.nextId();

        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertTrue(id2 > id1, "Subsequent ID should be greater than previous ID");
    }

    @Test
    @DisplayName("Should generate 50,000 unique IDs across 20 concurrent threads with ZERO collisions")
    void testConcurrentUniqueGeneration() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, 1);
        int threadCount = 20;
        int idsPerThread = 2500;
        int totalExpectedIds = threadCount * idsPerThread;

        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(totalExpectedIds, generatedIds.size(), "All generated IDs must be strictly unique without collision");
        assertEquals(totalExpectedIds, generator.getGeneratedCount());
    }
}
