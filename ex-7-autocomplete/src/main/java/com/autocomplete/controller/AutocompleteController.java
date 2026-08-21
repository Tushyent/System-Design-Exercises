package com.autocomplete.controller;

import com.autocomplete.dto.AutocompleteResponseDto;
import com.autocomplete.service.AutocompleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/autocomplete")
@Tag(name = "Autocomplete API", description = "Endpoints for prefix-based autocomplete search suggestions with Redis caching.")
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping
    @Operation(summary = "Get autocomplete suggestions", description = "Returns top-K matching suggestions for a given prefix. Checks cache first, falls back to Trie.")
    public ResponseEntity<AutocompleteResponseDto> getSuggestions(
            @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix,
            @RequestParam(value = "limit", required = false, defaultValue = "5") int limit) {
        
        AutocompleteResponseDto response = autocompleteService.getSuggestions(prefix, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @Operation(summary = "Submit a search query", description = "Increments search frequency for the term in the Trie and evicts relevant prefixes from Redis cache.")
    public ResponseEntity<String> submitSearch(@RequestBody Map<String, String> requestBody) {
        String word = requestBody.get("word");
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Request body must contain 'word' key with non-empty value.");
        }
        autocompleteService.processSearchSubmit(word);
        return ResponseEntity.ok("Search submitted for word '" + word.trim().toLowerCase() + "'. Frequency updated and cache invalidated.");
    }
}
