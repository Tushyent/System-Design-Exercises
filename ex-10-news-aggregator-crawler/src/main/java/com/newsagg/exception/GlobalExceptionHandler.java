package com.newsagg.exception;

import com.newsagg.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Question (b): reject with HTTP 429 + Retry-After when the bucket is empty. */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex,
                                                         HttpServletRequest request) {
        long retryAfter = computeRetryAfter(ex.getRemainingTokens(), ex.getRefillPerSecond());
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfter))
                .header("X-RateLimit-Remaining", String.valueOf(Math.max(0, (long) Math.floor(ex.getRemainingTokens()))))
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return error(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private static long computeRetryAfter(double remainingTokens, double refillPerSecond) {
        if (remainingTokens >= 1.0) {
            return 0L;
        }
        double seconds = (1.0 - remainingTokens) / Math.max(1e-9, refillPerSecond);
        return Math.max(1L, (long) Math.ceil(seconds));
    }

    private static ResponseEntity<ErrorResponse> error(HttpServletRequest request,
                                                       HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
