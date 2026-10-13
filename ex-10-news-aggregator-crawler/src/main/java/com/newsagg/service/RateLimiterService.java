package com.newsagg.service;

import com.newsagg.config.RateLimiterProperties;
import com.newsagg.exception.RateLimitExceededException;
import com.newsagg.ratelimit.BucketState;
import com.newsagg.ratelimit.BucketStatus;
import com.newsagg.ratelimit.BucketStore;
import com.newsagg.ratelimit.Decision;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final BucketStore bucketStore;
    private final RateLimiterProperties props;

    /**
     * Resolve the client identity, in priority order: {@code X-Client-Id}
     * header, {@code clientId} query parameter / body field, then remote IP.
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

    /** Try to consume one token for the client. */
    public Decision tryAcquire(String clientId) {
        return bucketStore.acquire(clientId, props.getCapacity(), props.getRefillPerSecond(),
                System.currentTimeMillis());
    }

    /** Enforce the limit; throws 429-mapped exception when the bucket is empty. */
    public Decision check(String clientId) {
        Decision d = tryAcquire(clientId);
        if (!d.allowed()) {
            throw new RateLimitExceededException(clientId, d.remainingTokens(), props.getRefillPerSecond());
        }
        return d;
    }

    /** Read-only view of a client's bucket (no token consumed). */
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
        log.info("Reset rate-limit bucket for client {}", clientId);
    }
}
