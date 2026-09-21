package com.crawler.service;

import com.crawler.model.CrawlTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class UrlFrontierQueue {

    private static final String REDIS_QUEUE_KEY = "crawl:queue";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Queue<CrawlTask> inMemoryQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean redisAvailable = true;

    public UrlFrontierQueue(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            this.redisAvailable = false;
        }
    }

    /**
     * Enqueue a new CrawlTask at the end of the frontier queue (FIFO).
     */
    public void enqueue(CrawlTask task) {
        if (task == null || task.getUrl() == null) {
            return;
        }

        if (redisAvailable) {
            try {
                redisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, task);
                return;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis frontier queue unavailable ({}); switching to in-memory queue fallback.", e.getMessage());
            }
        }
        inMemoryQueue.offer(task);
    }

    /**
     * Dequeue the next CrawlTask from the front of the frontier queue (FIFO).
     */
    public CrawlTask dequeue() {
        if (redisAvailable) {
            try {
                Object obj = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY);
                if (obj instanceof CrawlTask) {
                    return (CrawlTask) obj;
                } else if (obj instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                    return CrawlTask.builder()
                            .url((String) map.get("url"))
                            .depth(map.get("depth") != null ? ((Number) map.get("depth")).intValue() : 0)
                            .parentUrl((String) map.get("parentUrl"))
                            .discoveredTimestamp(map.get("discoveredTimestamp") != null ? ((Number) map.get("discoveredTimestamp")).longValue() : System.currentTimeMillis())
                            .build();
                }
                return null;
            } catch (Exception e) {
                redisAvailable = false;
                log.warn("Redis frontier dequeue failed ({}); falling back to in-memory queue.", e.getMessage());
            }
        }
        return inMemoryQueue.poll();
    }

    /**
     * Returns the approximate current number of pending URLs in the queue.
     */
    public int size() {
        if (redisAvailable) {
            try {
                Long sz = redisTemplate.opsForList().size(REDIS_QUEUE_KEY);
                return sz != null ? sz.intValue() : 0;
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        return inMemoryQueue.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Clears all pending tasks in both Redis and memory queues.
     */
    public void clear() {
        if (redisAvailable) {
            try {
                redisTemplate.delete(REDIS_QUEUE_KEY);
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        inMemoryQueue.clear();
    }

    /**
     * Inspects current tasks in the queue without removing them.
     */
    @SuppressWarnings("unchecked")
    public List<CrawlTask> peekAll(int limit) {
        List<CrawlTask> result = new ArrayList<>();
        if (redisAvailable) {
            try {
                List<Object> range = redisTemplate.opsForList().range(REDIS_QUEUE_KEY, 0, limit - 1);
                if (range != null) {
                    for (Object obj : range) {
                        if (obj instanceof CrawlTask) {
                            result.add((CrawlTask) obj);
                        } else if (obj instanceof java.util.Map) {
                            java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                            result.add(CrawlTask.builder()
                                    .url((String) map.get("url"))
                                    .depth(map.get("depth") != null ? ((Number) map.get("depth")).intValue() : 0)
                                    .parentUrl((String) map.get("parentUrl"))
                                    .build());
                        }
                    }
                    return result;
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        int count = 0;
        for (CrawlTask t : inMemoryQueue) {
            result.add(t);
            if (++count >= limit) break;
        }
        return result;
    }

    public boolean isUsingRedis() {
        return redisAvailable;
    }

    public void setRedisAvailable(boolean available) {
        this.redisAvailable = available;
    }
}
