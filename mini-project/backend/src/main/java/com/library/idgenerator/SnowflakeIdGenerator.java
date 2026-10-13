package com.library.idgenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 64-bit Distributed Snowflake Unique ID Generator
 *
 * Structure:
 * - 1 bit: Unused sign bit (always 0)
 * - 41 bits: Epoch timestamp (millis since 2026-01-01 00:00:00 UTC) (~69 years)
 * - 5 bits: Datacenter ID (0 - 31)
 * - 5 bits: Worker / Node ID (0 - 31)
 * - 12 bits: Sequence counter (0 - 4095 per millisecond)
 */
@Component
public class SnowflakeIdGenerator {

    // Custom epoch: 2026-01-01T00:00:00Z (1767225600000L)
    private static final long CUSTOM_EPOCH = 1767225600000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private final AtomicLong generatedCount = new AtomicLong(0);

    public SnowflakeIdGenerator(
            @Value("${snowflake.worker-id:1}") long workerId,
            @Value("${snowflake.datacenter-id:1}") long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long currentTimestamp = timeGen();

        // Clock rollback detection
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= 5) {
                // Minor clock drift: wait it out
                try {
                    Thread.sleep(offset + 1);
                    currentTimestamp = timeGen();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Clock moved backwards. Interrupted while waiting.", e);
                }
            } else {
                throw new IllegalStateException(String.format(
                        "Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTimestamp == currentTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence overflow within same millisecond -> wait next millisecond
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;
        generatedCount.incrementAndGet();

        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    public String nextIdString() {
        return Long.toString(nextId());
    }

    public String nextPrefixedId(String prefix) {
        return prefix + "-" + nextId();
    }

    public long getGeneratedCount() {
        return generatedCount.get();
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
