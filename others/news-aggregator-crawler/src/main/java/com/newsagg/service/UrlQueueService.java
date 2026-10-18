package com.newsagg.service;

import com.newsagg.config.CrawlerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * URL frontier — a FIFO queue of URLs waiting to be crawled.
 *
 * <p>Data structure: {@code Queue<String>} (FIFO). Redis is the primary store
 * (list {@code newscrawl:queue} via RPUSH/LPOP so state is centralised across
 * instances and survives restarts); a {@link ConcurrentLinkedQueue} is the
 * transparent fallback when Redis is unreachable.
 */
@Slf4j
@Service
public class UrlQueueService {

    private final StringRedisTemplate redis;
    private final CrawlerProperties props;
    private final Queue<String> memoryQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean redisAvailable = true;

    public UrlQueueService(StringRedisTemplate redis, CrawlerProperties props) {
        this.redis = redis;
        this.props = props;
    }

    /** Enqueue at the tail (FIFO). Silently ignores blanks/duplicates-in-queue. */
    public void enqueue(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (redisAvailable) {
            try {
                redis.opsForList().rightPush(props.getQueueKey(), url);
                return;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis queue unavailable ({}); using in-memory queue.", e.getMessage());
            }
        }
        if (!memoryQueue.contains(url)) {
            memoryQueue.offer(url);
        }
    }

    /** Dequeue from the head (FIFO); null when empty. */
    public String dequeue() {
        if (redisAvailable) {
            try {
                return redis.opsForList().leftPop(props.getQueueKey());
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis queue dequeue failed ({}); using in-memory queue.", e.getMessage());
            }
        }
        return memoryQueue.poll();
    }

    public int size() {
        if (redisAvailable) {
            try {
                Long sz = redis.opsForList().size(props.getQueueKey());
                return sz != null ? sz.intValue() : 0;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return memoryQueue.size();
    }

    /** Peek at pending URLs without removing them. */
    public List<String> peekAll(int limit) {
        if (redisAvailable) {
            try {
                List<String> range = redis.opsForList().range(props.getQueueKey(), 0, limit - 1);
                return range != null ? range : List.of();
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        List<String> out = new ArrayList<>();
        int count = 0;
        for (String u : memoryQueue) {
            out.add(u);
            if (++count >= limit) {
                break;
            }
        }
        return out;
    }

    public void clear() {
        if (redisAvailable) {
            try {
                redis.delete(props.getQueueKey());
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        memoryQueue.clear();
    }

    public boolean isUsingRedis() {
        return redisAvailable;
    }
}
