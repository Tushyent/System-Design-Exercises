package com.autocomplete.service;

import com.autocomplete.dto.SuggestionDto;
import com.autocomplete.dto.AutocompleteResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AutocompleteService {

    private final TrieService trieService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${autocomplete.cache.ttl-minutes:10}")
    private long cacheTtlMinutes;

    private static final String CACHE_KEY_PREFIX = "autocomplete:cache:";
    private static final int MAX_CACHE_LIMIT = 20;

    public AutocompleteService(TrieService trieService, RedisTemplate<String, Object> redisTemplate) {
        this.trieService = trieService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves autocomplete suggestions.
     * Checks Redis first. On cache miss, queries the Trie and caches the result.
     */
    @SuppressWarnings("unchecked")
    public AutocompleteResponseDto getSuggestions(String prefix, int limit) {
        long startTime = System.nanoTime();
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase().trim();
        String cacheKey = CACHE_KEY_PREFIX + normalizedPrefix;
        
        List<SuggestionDto> suggestions = null;
        String source = "CACHE";
        
        // Try to fetch from Redis
        try {
            List<Object> cachedList = (List<Object>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedList != null) {
                suggestions = new ArrayList<>();
                for (Object obj : cachedList) {
                    if (obj instanceof java.util.LinkedHashMap) {
                        java.util.LinkedHashMap<?, ?> map = (java.util.LinkedHashMap<?, ?>) obj;
                        String word = (String) map.get("word");
                        int freq = (Integer) map.get("frequency");
                        suggestions.add(new SuggestionDto(word, freq));
                    } else if (obj instanceof SuggestionDto) {
                        suggestions.add((SuggestionDto) obj);
                    }
                }
                log.info("Cache hit for prefix: '{}'", normalizedPrefix);
            }
        } catch (Exception e) {
            log.warn("Redis is unavailable or error reading from cache: {}. Falling back to Trie.", e.getMessage());
        }
        
        // On cache miss, search the Trie and cache the results
        if (suggestions == null) {
            source = "TRIE";
            suggestions = trieService.search(normalizedPrefix, MAX_CACHE_LIMIT);
            
            // Cache the results in Redis
            try {
                redisTemplate.opsForValue().set(cacheKey, suggestions, cacheTtlMinutes, TimeUnit.MINUTES);
                log.info("Cache miss for prefix: '{}'. Stored {} results in Redis.", normalizedPrefix, suggestions.size());
            } catch (Exception e) {
                log.warn("Failed to write to Redis cache: {}", e.getMessage());
            }
        }
        
        // Limit to the requested size
        List<SuggestionDto> responseSuggestions = suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        long executionTimeNs = System.nanoTime() - startTime;
        long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(executionTimeNs);
        // Fallback to 1ms if it's rounded down to 0, to make it distinct in logs
        if (executionTimeMs == 0 && executionTimeNs > 0) {
            executionTimeMs = 1;
        }
        
        return new AutocompleteResponseDto(normalizedPrefix, responseSuggestions, source, executionTimeMs);
    }

    /**
     * Increments the frequency of a word and invalidates all its prefixes in the cache.
     */
    public void processSearchSubmit(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        String normalizedWord = word.toLowerCase().trim();
        log.info("Processing search submission for word: '{}'", normalizedWord);
        
        // 1. Update frequency in memory Trie
        trieService.incrementFrequency(normalizedWord, 1);
        
        // 2. Invalidate prefix cache keys in Redis
        invalidateCacheForPrefixes(normalizedWord);
    }

    private void invalidateCacheForPrefixes(String word) {
        try {
            List<String> keysToEvict = new ArrayList<>();
            // Include empty prefix cache
            keysToEvict.add(CACHE_KEY_PREFIX);
            
            for (int i = 1; i <= word.length(); i++) {
                String prefix = word.substring(0, i);
                keysToEvict.add(CACHE_KEY_PREFIX + prefix);
            }
            
            redisTemplate.delete(keysToEvict);
            log.info("Evicted {} cache keys from Redis for word: '{}'", keysToEvict.size(), word);
        } catch (Exception e) {
            log.warn("Failed to evict cache keys from Redis: {}", e.getMessage());
        }
    }
}
