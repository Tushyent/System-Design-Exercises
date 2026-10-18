package com.library.autocomplete;

import com.library.common.dto.ApiResponse;
import com.library.common.exception.RateLimitExceededException;
import com.library.ratelimit.RateLimiterDecision;
import com.library.ratelimit.TokenBucketRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/autocomplete")
@RequiredArgsConstructor
public class AutocompleteController {

    /** Public read policy: matching burst/refill of the catalogue read endpoints. */
    @Value("${ratelimit.search.capacity:1500}")
    private int readBurstCapacity;

    @Value("${ratelimit.search.refill-per-second:750}")
    private int readRefillPerSecond;

    private final AutocompleteService autocompleteService;
    private final TokenBucketRateLimiter rateLimiter;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest httpRequest) {

        RateLimiterDecision decision = rateLimiter.tryAcquire(
                "search:" + httpRequest.getRemoteAddr(), readBurstCapacity, readRefillPerSecond);
        if (!decision.isAllowed()) {
            throw new RateLimitExceededException(
                    "Autocomplete rate limit exceeded. Please slow down.", decision.getRetryAfterSeconds());
        }

        long start = System.currentTimeMillis();
        List<AutocompleteSuggestion> suggestions = autocompleteService.getSuggestions(prefix, limit);
        long latencyMs = System.currentTimeMillis() - start;

        Map<String, Object> response = new HashMap<>();
        response.put("prefix", prefix);
        response.put("limit", limit);
        response.put("count", suggestions.size());
        response.put("latencyMs", latencyMs);
        response.put("suggestions", suggestions);

        return ResponseEntity.ok(ApiResponse.ok(response, "Suggestions retrieved"));
    }

    @GetMapping("/evaluate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluateAccuracy(
            @RequestParam(defaultValue = "5") int k) {
        Map<String, Object> report = autocompleteService.evaluateAccuracy(k);
        return ResponseEntity.ok(ApiResponse.ok(report, "Autocomplete accuracy evaluation completed"));
    }
}
