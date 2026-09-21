package com.crawler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VisitedUrlService {

    private static final String REDIS_VISITED_KEY = "crawl:visited";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Set<String> inMemoryVisited = ConcurrentHashMap.newKeySet();
    private volatile boolean redisAvailable = true;

    public VisitedUrlService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            this.redisAvailable = false;
        }
    }

    /**
     * Atomically checks if a URL has already been visited and marks it as visited.
     * Returns TRUE if the URL was NOT visited before (successfully added).
     * Returns FALSE if the URL had ALREADY been visited (duplicate avoided).
     */
    public boolean markVisitedIfAbsent(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        if (redisAvailable) {
            try {
                Long added = redisTemplate.opsForSet().add(REDIS_VISITED_KEY, url);
                boolean isNew = added != null && added > 0;
                // Also mirror to memory for instant local lookup
                inMemoryVisited.add(url);
                return isNew;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis visited set unavailable ({}); switching to in-memory set.", e.getMessage());
            }
        }
        return inMemoryVisited.add(url);
    }

    /**
     * Checks whether a URL is present in the visited set.
     */
    public boolean isVisited(String url) {
        if (url == null) return false;

        if (redisAvailable) {
            try {
                Boolean member = redisTemplate.opsForSet().isMember(REDIS_VISITED_KEY, url);
                return Boolean.TRUE.equals(member);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return inMemoryVisited.contains(url);
    }

    /**
     * Returns the total count of distinct visited URLs.
     */
    public int getVisitedCount() {
        if (redisAvailable) {
            try {
                Long sz = redisTemplate.opsForSet().size(REDIS_VISITED_KEY);
                return sz != null ? sz.intValue() : 0;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return inMemoryVisited.size();
    }

    /**
     * Returns all visited URLs.
     */
    public Set<String> getAllVisited() {
        if (redisAvailable) {
            try {
                Set<Object> members = redisTemplate.opsForSet().members(REDIS_VISITED_KEY);
                if (members != null) {
                    return members.stream().map(Object::toString).collect(Collectors.toSet());
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return Collections.unmodifiableSet(inMemoryVisited);
    }

    /**
     * Clears all visited URLs in both Redis and memory.
     */
    public void clear() {
        if (redisAvailable) {
            try {
                redisTemplate.delete(REDIS_VISITED_KEY);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        inMemoryVisited.clear();
    }

    public boolean isUsingRedis() {
        return redisAvailable;
    }

    public void setRedisAvailable(boolean available) {
        this.redisAvailable = available;
    }
}
