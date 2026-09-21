package com.crawler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to detect identical or near-duplicate web content across different URLs
 * using cryptographic checksums (SHA-256) of normalized body text.
 */
@Slf4j
@Service
public class ContentDeduplicationService {

    private static final String CONTENT_HASH_PREFIX = "crawl:content:hash:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, String> inMemoryHashToUrl = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public ContentDeduplicationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            this.redisAvailable = false;
        }
    }

    /**
     * Computes a SHA-256 hash string for the given normalized text.
     */
    public String computeContentHash(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        // Normalize: lowercase and condense all whitespace runs
        String normalized = text.toLowerCase().replaceAll("\\s+", " ").trim();
        if (normalized.length() < 20) {
            // Content too small to reliably deduplicate
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Error computing content hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if this content hash has already been registered by a previously crawled URL.
     * If not registered, records (hash -> currentUrl) and returns null (not a duplicate).
     * If already registered, returns the canonical/original URL that was registered first.
     */
    public String checkAndRegisterHash(String contentHash, String currentUrl) {
        if (contentHash == null || currentUrl == null) {
            return null;
        }

        if (redisAvailable) {
            try {
                String key = CONTENT_HASH_PREFIX + contentHash;
                Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, currentUrl);
                if (Boolean.TRUE.equals(wasSet)) {
                    inMemoryHashToUrl.put(contentHash, currentUrl);
                    return null; // Successfully registered as original
                } else {
                    Object existingUrl = redisTemplate.opsForValue().get(key);
                    return existingUrl != null ? existingUrl.toString() : inMemoryHashToUrl.get(contentHash);
                }
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis content hash check failed ({}); using memory fallback.", e.getMessage());
            }
        }

        String existing = inMemoryHashToUrl.putIfAbsent(contentHash, currentUrl);
        return existing; // returns null if currentUrl was added, or existing original URL if duplicate
    }

    public void clear() {
        if (redisAvailable) {
            try {
                for (String hash : inMemoryHashToUrl.keySet()) {
                    redisTemplate.delete(CONTENT_HASH_PREFIX + hash);
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        inMemoryHashToUrl.clear();
    }
}
