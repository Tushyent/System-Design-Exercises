package com.library.shorturl;

import com.library.common.exception.ResourceNotFoundException;
import com.library.idgenerator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final AtomicLong totalResolutions = new AtomicLong(0);
    private final AtomicLong totalLookupNanos = new AtomicLong(0);

    public ShortUrl shorten(String targetUrl, String resourceType, String resourceId) {
        // If already exists, return existing
        if (resourceType != null && resourceId != null) {
            Optional<ShortUrl> existing = shortUrlRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        long uniqueNumber = idGenerator.nextId();
        String shortCode = Base62Encoder.encode(uniqueNumber);

        // Ensure uniqueness
        while (shortUrlRepository.findByShortCode(shortCode).isPresent()) {
            uniqueNumber = idGenerator.nextId();
            shortCode = Base62Encoder.encode(uniqueNumber);
        }

        ShortUrl shortUrl = ShortUrl.builder()
                .id(idGenerator.nextIdString())
                .shortCode(shortCode)
                .targetUrl(targetUrl)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();

        ShortUrl saved = shortUrlRepository.save(shortUrl);

        // Populate Redis cache
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("shorturl:" + shortCode, targetUrl, Duration.ofDays(7));
            } catch (Exception e) {
                log.warn("Redis write failed for shorturl {}: {}", shortCode, e.getMessage());
            }
        }

        return saved;
    }

    public String resolve(String shortCode) {
        long start = System.nanoTime();
        totalResolutions.incrementAndGet();

        String targetUrl = null;

        // 1. Check Redis Cache
        if (redisTemplate != null) {
            try {
                targetUrl = redisTemplate.opsForValue().get("shorturl:" + shortCode);
            } catch (Exception e) {
                log.warn("Redis shorturl cache read failed: {}", e.getMessage());
            }
        }

        // 2. Fallback to MongoDB
        if (targetUrl == null) {
            ShortUrl record = shortUrlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Short URL code not found or expired: " + shortCode));

            targetUrl = record.getTargetUrl();
            record.setClickCount(record.getClickCount() + 1);
            shortUrlRepository.save(record);

            // Populate Redis
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set("shorturl:" + shortCode, targetUrl, Duration.ofDays(7));
                } catch (Exception e) {
                    log.warn("Redis shorturl cache write failed: {}", e.getMessage());
                }
            }
        }

        long elapsedNanos = System.nanoTime() - start;
        totalLookupNanos.addAndGet(elapsedNanos);

        return targetUrl;
    }

    public double getAverageResolutionLatencyMs() {
        long count = totalResolutions.get();
        return count > 0 ? (totalLookupNanos.get() / (double) count) / 1_000_000.0 : 0.0;
    }

    public long getTotalResolutions() {
        return totalResolutions.get();
    }
}
