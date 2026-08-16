package com.ratelimiter.controller;

import com.ratelimiter.dto.HelloResponse;
import com.ratelimiter.ratelimit.BucketStatus;
import com.ratelimiter.ratelimit.Decision;
import com.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Demo API protected by the token-bucket rate limiter. The client is identified
 * by the {@code X-Client-Id} header, the {@code clientId} query parameter, or the
 * caller's IP address (in that priority order).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final RateLimiterService service;

    /** Rate-limited resource: returns HTTP 200 while tokens remain, 429 otherwise. */
    @GetMapping("/hello")
    public ResponseEntity<HelloResponse> hello(HttpServletRequest request) {
        String clientId = service.resolveClientId(request);
        Decision d = service.check(clientId);
        HelloResponse body = new HelloResponse(
                clientId,
                "Hello, " + clientId + "! Request served by the token-bucket rate limiter.",
                d.remainingTokens(),
                service.status(clientId).capacity(),
                service.status(clientId).refillPerSecond(),
                LocalDateTime.now());
        return ResponseEntity.ok(body);
    }

    /** Read-only view of a client's bucket (no token consumed). */
    @GetMapping("/bucket/{clientId}")
    public BucketStatus bucketStatus(@PathVariable String clientId) {
        return service.status(clientId);
    }

    /** Drop a client's bucket so the demo can be replayed from a full bucket. */
    @DeleteMapping("/bucket/{clientId}")
    public ResponseEntity<Void> resetBucket(@PathVariable String clientId) {
        service.reset(clientId);
        return ResponseEntity.noContent().build();
    }
}