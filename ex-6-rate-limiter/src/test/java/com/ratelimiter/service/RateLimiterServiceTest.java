package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.dto.ConcurrencyTestRequest;
import com.ratelimiter.dto.ConcurrencyTestResult;
import com.ratelimiter.exception.RateLimitExceededException;
import com.ratelimiter.ratelimit.BucketStatus;
import com.ratelimiter.ratelimit.Decision;
import com.ratelimiter.ratelimit.InMemoryBucketStore;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        RateLimiterProperties props = new RateLimiterProperties();
        props.setCapacity(10);
        props.setRefillPerSecond(5.0);
        props.setStore("memory");
        service = new RateLimiterService(new InMemoryBucketStore(), props);
    }

    @Test
    void checkAcceptsUpToCapacityThenThrows429() {
        for (int i = 0; i < 10; i++) {
            Decision d = service.check("alice");
            assertTrue(d.allowed());
        }
        assertThrows(RateLimitExceededException.class, () -> service.check("alice"));
    }

    @Test
    void statusReportsRecoveringTokensAfterDrain() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            service.tryAcquire("alice");
        }
        Thread.sleep(600);
        BucketStatus status = service.status("alice");
        assertTrue(status.currentTokens() >= 2.0, "refill of 5/s should restore ~3 tokens after 600ms");
        assertEquals(0, status.retryAfterSeconds());
    }

    @Test
    void statusShowsRetryAfterWhenDrained() {
        for (int i = 0; i < 10; i++) {
            service.tryAcquire("alice", 10, 0.0);
        }
        BucketStatus status = service.status("alice");
        assertEquals(0.0, status.currentTokens(), 1e-9);
        assertTrue(status.retryAfterSeconds() >= 1);
    }

    @Test
    void resetRestoresFullBucket() {
        for (int i = 0; i < 15; i++) {
            service.tryAcquire("alice", 10, 0.0);
        }
        service.reset("alice");
        assertTrue(service.tryAcquire("alice", 10, 0.0).allowed());
    }

    @Test
    void concurrentTestIsLossless() {
        ConcurrencyTestResult result = service.runConcurrentTest(
                new ConcurrencyTestRequest("load", 20, 20, 5, 100.0));
        assertEquals(400, result.totalRequests());
        assertEquals(result.totalRequests(), result.allowed() + result.rejected(),
                "atomic acquire must never drop a request from the count");
        assertTrue(result.allowed() >= 1);
        assertTrue(result.allowed() <= 5 + 200, "burst admitted is bounded by capacity + refill");
    }

    @Test
    void resolveClientIdPrecedence() {
        HttpServletRequest headerOnly = mock(HttpServletRequest.class);
        when(headerOnly.getHeader("X-Client-Id")).thenReturn("header-client");
        assertEquals("header-client", service.resolveClientId(headerOnly));

        HttpServletRequest paramOnly = mock(HttpServletRequest.class);
        when(paramOnly.getParameter("clientId")).thenReturn("param-client");
        assertEquals("param-client", service.resolveClientId(paramOnly));

        HttpServletRequest ipOnly = mock(HttpServletRequest.class);
        when(ipOnly.getRemoteAddr()).thenReturn("10.0.0.7");
        assertEquals("10.0.0.7", service.resolveClientId(ipOnly));

        HttpServletRequest both = mock(HttpServletRequest.class);
        when(both.getHeader("X-Client-Id")).thenReturn("h");
        when(both.getParameter("clientId")).thenReturn("p");
        assertEquals("h", service.resolveClientId(both), "header wins over param");

        assertNotNull(service.resolveClientId(mock(HttpServletRequest.class)));
    }
}