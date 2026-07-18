package com.example.urlshortener_red.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.urlshortener_red.model.URLModel;
import com.example.urlshortener_red.repository.URLRepo;

@Service
public class URLService {

    @Autowired
    private URLRepo repo;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Create Short URL
    public String createShortURL(String longUrl) {

        // Add protocol if missing
        if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
            longUrl = "https://" + longUrl;
        }

        // Check if URL already exists
        Optional<URLModel> existing = repo.findByLongUrl(longUrl);

        if (existing.isPresent()) {
            return existing.get().getShortCode();
        }

        String shortCode = generateShortCode(longUrl);

        // Collision Handling
        while (repo.existsByShortCode(shortCode)) {

            shortCode = generateShortCode(longUrl + System.currentTimeMillis());

        }

        URLModel model = new URLModel();

        model.setLongUrl(longUrl);
        model.setShortCode(shortCode);

        repo.save(model);

        // Store in Redis
        redisTemplate.opsForValue().set(shortCode, longUrl);

        return shortCode;
    }

    // Cache Aside Strategy
    public Map<String, Object> getLongURL(String shortCode) {

        Map<String, Object> response = new HashMap<>();

        long start = System.nanoTime();

        // Check Redis
        String cachedURL = redisTemplate.opsForValue().get(shortCode);

        if (cachedURL != null) {

            long end = System.nanoTime();

            response.put("longUrl", cachedURL);
            response.put("cache", "Cache Hit");
            response.put("responseTime",
                    ((end - start) / 1_000_000.0) + " ms");

            return response;
        }

        // Cache Miss
        Optional<URLModel> model = repo.findByShortCode(shortCode);

        if (model.isPresent()) {

            String longUrl = model.get().getLongUrl();

            redisTemplate.opsForValue().set(shortCode, longUrl);

            long end = System.nanoTime();

            response.put("longUrl", longUrl);
            response.put("cache", "Cache Miss");
            response.put("responseTime",
                    ((end - start) / 1_000_000.0) + " ms");

            return response;
        }

        return null;
    }

    private String generateShortCode(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}