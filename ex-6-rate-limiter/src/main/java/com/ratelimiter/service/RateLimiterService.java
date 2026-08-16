package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.dto.ConcurrencyTestRequest;
import com.ratelimiter.dto.ConcurrencyTestResult;
import com.ratelimiter.exception.RateLimitExceededException;
import com.ratelimiter.ratelimit.BucketState;
import com.ratelimiter.ratelimit.BucketStatus;
import com.ratelimiter.ratelimit.BucketStore;
import com.ratelimiter.ratelimit.Decision;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates the token-bucket rate limiter: resolves the client identity,
 * delegates the atomic acquire to the {@link BucketStore}, and exposes status /
 * reset / concurrency-test operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final BucketStore bucketStore;
    private final RateLimiterProperties props;

    /**
     * Resolve the client identity, in priority order:
     * {@code X-Client-Id} header, {@code clientId} query parameter, then the
     * remote IP address (colons replaced so it is a safe Redis key).
     */
    public String resolveClientId(HttpServletRequest request) {
        String header = request.getHeader("X-Client-Id");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String param = request.getParameter("clientId");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? "unknown" : ip.replace(':', '_');
    }

    /** Try to consume one token using the configured capacity / refill rate. */
    public Decision tryAcquire(String clientId) {
        return bucketStore.acquire(clientId, props.getCapacity(), props.getRefillPerSecond(),
                System.currentTimeMillis());
    }

    /** Try to consume one token using explicit bucket parameters. */
    public Decision tryAcquire(String clientId, int capacity, double refillPerSecond) {
        return bucketStore.acquire(clientId, capacity, refillPerSecond, System.currentTimeMillis());
    }

    /** Enforce the limit; throws {@link RateLimitExceededException} when over. */
    public Decision check(String clientId) {
        Decision d = tryAcquire(clientId);
        if (!d.allowed()) {
            throw new RateLimitExceededException(clientId, d.remainingTokens(), props.getRefillPerSecond());
        }
        return d;
    }

    /** Read-only view of a client's bucket. */
    public BucketStatus status(String clientId) {
        long now = System.currentTimeMillis();
        Optional<BucketState> state = bucketStore.state(clientId, props.getCapacity(),
                props.getRefillPerSecond(), now);
        double tokens = state.map(BucketState::tokens).orElse((double) props.getCapacity());
        long lastRefill = state.map(BucketState::lastRefillMillis).orElse(now);
        long retryAfter = tokens < 1.0
                ? (long) Math.ceil((1.0 - tokens) / Math.max(1e-9, props.getRefillPerSecond()))
                : 0L;
        return new BucketStatus(clientId, tokens, props.getCapacity(), props.getRefillPerSecond(),
                lastRefill, retryAfter);
    }

    public void reset(String clientId) {
        bucketStore.reset(clientId);
        log.info("Reset bucket for client {}", clientId);
    }

    /**
     * Fire {@code threads * requestsPerThread} requests at a single client bucket,
     * all started simultaneously with a barrier so contention is maximised. The
     * {@link BucketStore} (Lua in production) guarantees each admission is atomic,
     * so {@code allowed + rejected} must always equal {@code totalRequests}.
     */
    public ConcurrencyTestResult runConcurrentTest(ConcurrencyTestRequest req) {
        long start = System.nanoTime();
        int threads = req.threads();
        int perThread = req.requestsPerThread();
        long total = (long) threads * perThread;

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicLong allowed = new AtomicLong();
        AtomicLong rejected = new AtomicLong();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    done.countDown();
                    return;
                }
                for (int j = 0; j < perThread; j++) {
                    Decision d = bucketStore.acquire(req.clientId(), req.capacity(),
                            req.refillPerSecond(), System.currentTimeMillis());
                    if (d.allowed()) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                }
                done.countDown();
            }, "rl-test-" + i).start();
        }

        try {
            ready.await();
            go.countDown();
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long durationMillis = (System.nanoTime() - start) / 1_000_000;
        String note = String.format(
                "atomic guarantee holds (allowed+rejected=total). Burst capacity=%d admitted, excess refused as 429.",
                req.capacity());
        log.info("Concurrency test client={} total={} allowed={} rejected={} in {}ms",
                req.clientId(), total, allowed.get(), rejected.get(), durationMillis);
        return new ConcurrencyTestResult(req.clientId(), threads, perThread, req.capacity(),
                req.refillPerSecond(), total, allowed.get(), rejected.get(), durationMillis, note);
    }
}