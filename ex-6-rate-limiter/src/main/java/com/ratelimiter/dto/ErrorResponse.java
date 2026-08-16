package com.ratelimiter.dto;

import java.time.LocalDateTime;

/**
 * JSON body returned for HTTP 429 (Too Many Requests) and other failures.
 */
public record ErrorResponse(LocalDateTime timestamp,
                            int status,
                            String error,
                            String message,
                            String path) {
}