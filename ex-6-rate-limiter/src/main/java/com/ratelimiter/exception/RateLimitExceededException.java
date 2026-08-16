package com.ratelimiter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final String clientId;
    private final double remainingTokens;
    private final double refillPerSecond;

    public RateLimitExceededException(String clientId, double remainingTokens, double refillPerSecond) {
        super("Rate limit exceeded for client '" + clientId + "'");
        this.clientId = clientId;
        this.remainingTokens = remainingTokens;
        this.refillPerSecond = refillPerSecond;
    }
}