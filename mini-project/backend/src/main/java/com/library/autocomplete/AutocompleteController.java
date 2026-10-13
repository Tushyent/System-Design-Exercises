package com.library.autocomplete;

import com.library.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/autocomplete")
@RequiredArgsConstructor
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "8") int limit) {

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
