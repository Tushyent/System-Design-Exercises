package com.crawler.service;

import com.crawler.config.CrawlerProperties;
import com.crawler.model.PageMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PageCacheService {

    private static final String PAGE_KEY_PREFIX = "crawl:page:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CrawlerProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, PageMetadata> inMemoryCache = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public PageCacheService(RedisTemplate<String, Object> redisTemplate, CrawlerProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        if (redisTemplate == null) {
            this.redisAvailable = false;
        }
    }

    /**
     * Stores page metadata and discovered links into Redis cache and in-memory store.
     */
    public void cachePage(PageMetadata page) {
        if (page == null || page.getUrl() == null) return;

        inMemoryCache.put(page.getUrl(), page);

        if (redisAvailable) {
            try {
                String key = PAGE_KEY_PREFIX + page.getUrl();
                redisTemplate.opsForValue().set(key, page, properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis page cache write failed ({}); maintaining in-memory cache.", e.getMessage());
            }
        }
    }

    /**
     * Retrieves page metadata from Redis cache first; falls back to in-memory cache.
     */
    public PageMetadata getCachedPage(String url) {
        if (url == null) return null;

        if (redisAvailable) {
            try {
                String key = PAGE_KEY_PREFIX + url;
                Object obj = redisTemplate.opsForValue().get(key);
                if (obj instanceof PageMetadata) {
                    PageMetadata pm = (PageMetadata) obj;
                    pm.setCached(true);
                    return pm;
                } else if (obj instanceof Map) {
                    PageMetadata pm = objectMapper.convertValue(obj, PageMetadata.class);
                    pm.setCached(true);
                    return pm;
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }

        PageMetadata memoryPage = inMemoryCache.get(url);
        if (memoryPage != null) {
            memoryPage.setCached(true);
        }
        return memoryPage;
    }

    public boolean hasCachedPage(String url) {
        return getCachedPage(url) != null;
    }

    /**
     * Returns all currently stored pages.
     */
    public List<PageMetadata> getAllPages() {
        return new ArrayList<>(inMemoryCache.values());
    }

    public int getCachedPageCount() {
        return inMemoryCache.size();
    }

    /**
     * Clears all cached pages.
     */
    public void clear() {
        if (redisAvailable) {
            try {
                for (String url : inMemoryCache.keySet()) {
                    redisTemplate.delete(PAGE_KEY_PREFIX + url);
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        inMemoryCache.clear();
    }

    public boolean isUsingRedis() {
        return redisAvailable;
    }

    public void setRedisAvailable(boolean available) {
        this.redisAvailable = available;
    }
}
