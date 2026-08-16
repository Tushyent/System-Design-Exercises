package com.ratelimiter.dto;

import java.time.LocalDateTime;

/**
 * Successful (HTTP 200) response of the rate-limited endpoint.
 *
 * @param clientId        the identified client
 * @param message         the resource payload
 * @param remaining       tokens left in the bucket after this request
 * @param limit           bucket capacity
 * @param refillPerSecond configured refill rate
 * @param timestamp       server time of the response
 */
public record HelloResponse(String clientId,
                            String message,
                            double remaining,
                            int limit,
                            double refillPerSecond,
                            LocalDateTime timestamp) {
}