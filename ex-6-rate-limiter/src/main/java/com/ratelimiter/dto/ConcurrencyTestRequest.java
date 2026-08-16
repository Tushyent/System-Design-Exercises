package com.ratelimiter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body of the in-app concurrency stress test endpoint.
 *
 * @param clientId           the shared client whose bucket is stressed
 * @param threads            number of concurrent worker threads
 * @param requestsPerThread  requests issued by each worker
 * @param capacity           bucket capacity used for the test
 * @param refillPerSecond    refill rate used for the test
 */
public record ConcurrencyTestRequest(
        @NotBlank String clientId,
        @NotNull @Min(1) @Max(100) Integer threads,
        @NotNull @Min(1) @Max(10000) Integer requestsPerThread,
        @NotNull @Min(1) @Max(10000) Integer capacity,
        @NotNull @Positive Double refillPerSecond) {
}