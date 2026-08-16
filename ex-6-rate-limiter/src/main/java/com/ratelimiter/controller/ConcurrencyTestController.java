package com.ratelimiter.controller;

import com.ratelimiter.dto.ConcurrencyTestRequest;
import com.ratelimiter.dto.ConcurrencyTestResult;
import com.ratelimiter.service.RateLimiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-app concurrency stress test: fires many threads at a single client bucket to
 * demonstrate that the atomic Lua token update keeps the rate limit correct under
 * simultaneous requests.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class ConcurrencyTestController {

    private final RateLimiterService service;

    @PostMapping("/concurrent")
    public ConcurrencyTestResult concurrent(@Valid @RequestBody ConcurrencyTestRequest request) {
        return service.runConcurrentTest(request);
    }
}