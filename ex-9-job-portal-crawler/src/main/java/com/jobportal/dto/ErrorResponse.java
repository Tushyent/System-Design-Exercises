package com.jobportal.dto;

import java.time.LocalDateTime;

/** Standard error body (also used for the 429 rate-limit response). */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}
