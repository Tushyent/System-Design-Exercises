package com.library.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimiterDecision {
    private boolean allowed;
    private double remainingTokens;
    private long retryAfterSeconds;
    private long executionTimeNanos;
}
