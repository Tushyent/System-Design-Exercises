package com.ratelimiter.dto;

/**
 * Result summary of the in-app concurrency stress test.
 *
 * @param clientId          the shared client used for the test
 * @param threads           number of concurrent workers
 * @param requestsPerThread requests per worker
 * @param capacity          bucket capacity used
 * @param refillPerSecond   refill rate used
 * @param totalRequests     threads * requestsPerThread
 * @param allowed           requests admitted (HTTP 200)
 * @param rejected          requests refused (HTTP 429)
 * @param durationMillis    wall-clock time of the test
 * @param note              human-readable interpretation
 */
public record ConcurrencyTestResult(String clientId,
                                    int threads,
                                    int requestsPerThread,
                                    int capacity,
                                    double refillPerSecond,
                                    long totalRequests,
                                    long allowed,
                                    long rejected,
                                    long durationMillis,
                                    String note) {
}