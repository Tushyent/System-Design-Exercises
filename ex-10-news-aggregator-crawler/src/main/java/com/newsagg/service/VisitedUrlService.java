package com.newsagg.service;

import com.newsagg.config.CrawlerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visited-URL set — prevents the same news article from being crawled twice.
 *
 * <p>Data structure: {@code Set<String>} (HashSet semantics). Redis is the
 * primary store (set {@code newscrawl:visited} via SADD/SISMEMBER so duplicates
 * are suppressed centrally even with concurrent clients); a
 * {@code ConcurrentHashMap}-backed set is the transparent fallback.
 */
@Slf4j
@Service
public class VisitedUrlService {

    private final StringRedisTemplate redis;
    private final CrawlerProperties props;
    private final Set<String> memoryVisited = ConcurrentHashMap.newKeySet();
    private volatile boolean redisAvailable = true;

    public VisitedUrlService(StringRedisTemplate redis, CrawlerProperties props) {
        this.redis = redis;
        this.props = props;
    }

    /**
     * Atomically mark a URL visited. Returns true when the URL is NEW
     * (caller should crawl it); false when it was ALREADY visited (skip).
     */
    public boolean markVisitedIfAbsent(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        if (redisAvailable) {
            try {
                Long added = redis.opsForSet().add(props.getVisitedKey(), url);
                boolean isNew = added != null && added > 0;
                memoryVisited.add(url); // local mirror for fast lookup
                return isNew;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis visited set unavailable ({}); using in-memory set.", e.getMessage());
            }
        }
        return memoryVisited.add(url);
    }

    public boolean isVisited(String url) {
        if (url == null) {
            return false;
        }
        if (redisAvailable) {
            try {
                return Boolean.TRUE.equals(redis.opsForSet().isMember(props.getVisitedKey(), url));
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return memoryVisited.contains(url);
    }

    public int getVisitedCount() {
        if (redisAvailable) {
            try {
                Long sz = redis.opsForSet().size(props.getVisitedKey());
                return sz != null ? sz.intValue() : 0;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return memoryVisited.size();
    }

    /** All visited URLs, sorted for stable API output. */
    public Set<String> getAllVisited() {
        if (redisAvailable) {
            try {
                Set<String> members = redis.opsForSet().members(props.getVisitedKey());
                if (members != null) {
                    return new TreeSet<>(members);
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return new TreeSet<>(memoryVisited);
    }

    public void clear() {
        if (redisAvailable) {
            try {
                redis.delete(props.getVisitedKey());
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        memoryVisited.clear();
    }

    public boolean isUsingRedis() {
        return redisAvailable;
    }
}
