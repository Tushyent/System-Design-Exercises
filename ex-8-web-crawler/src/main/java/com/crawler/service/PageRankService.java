package com.crawler.service;

import com.crawler.model.PageMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service implementing Google's PageRank algorithm to compute link-analysis authority scores
 * for crawled web pages. Uses power iteration with damping factor (0.85) and dangling node handling.
 */
@Slf4j
@Service
public class PageRankService {

    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 50;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;
    private static final String REDIS_PAGERANK_PREFIX = "crawl:pagerank:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Double> inMemoryPageRank = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public PageRankService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            this.redisAvailable = false;
        }
    }

    /**
     * Executes the PageRank power iteration algorithm on the collection of crawled pages.
     */
    public synchronized Map<String, Double> computePageRank(List<PageMetadata> pages) {
        if (pages == null || pages.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> allUrls = new HashSet<>();
        Map<String, Set<String>> outLinks = new HashMap<>();
        Map<String, Set<String>> inLinks = new HashMap<>();

        for (PageMetadata p : pages) {
            String url = p.getUrl();
            allUrls.add(url);
            outLinks.putIfAbsent(url, new HashSet<>());
            inLinks.putIfAbsent(url, new HashSet<>());
        }

        for (PageMetadata p : pages) {
            String src = p.getUrl();
            if (p.getDiscoveredLinks() != null) {
                for (String dst : p.getDiscoveredLinks()) {
                    if (allUrls.contains(dst)) {
                        outLinks.get(src).add(dst);
                        inLinks.get(dst).add(src);
                    }
                }
            }
        }

        int N = allUrls.size();
        if (N == 0) return Collections.emptyMap();

        Map<String, Double> pr = new HashMap<>();
        double initialRank = 1.0 / N;
        for (String url : allUrls) {
            pr.put(url, initialRank);
        }

        int iteration = 0;
        for (iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            Map<String, Double> nextPr = new HashMap<>();
            double danglingSum = 0.0;

            for (String url : allUrls) {
                if (outLinks.get(url).isEmpty()) {
                    danglingSum += pr.get(url);
                }
            }

            double baseRank = (1.0 - DAMPING_FACTOR) / N + (DAMPING_FACTOR * danglingSum / N);

            for (String url : allUrls) {
                double incomingSum = 0.0;
                for (String incoming : inLinks.get(url)) {
                    int outDegree = outLinks.get(incoming).size();
                    if (outDegree > 0) {
                        incomingSum += pr.get(incoming) / outDegree;
                    }
                }
                nextPr.put(url, baseRank + (DAMPING_FACTOR * incomingSum));
            }

            // Check convergence (L1 norm delta)
            double delta = 0.0;
            for (String url : allUrls) {
                delta += Math.abs(nextPr.get(url) - pr.get(url));
            }

            pr = nextPr;
            if (delta < CONVERGENCE_THRESHOLD) {
                iteration++;
                break;
            }
        }

        // Normalize PageRank scores to percentage scale [0.0 - 100.0] for intuitive display
        double maxRank = pr.values().stream().max(Double::compareTo).orElse(1.0);
        inMemoryPageRank.clear();

        for (Map.Entry<String, Double> entry : pr.entrySet()) {
            double normalized = maxRank > 0 ? (entry.getValue() / maxRank) * 100.0 : 1.0;
            double score = Math.round(normalized * 100.0) / 100.0;
            inMemoryPageRank.put(entry.getKey(), score);

            if (redisAvailable) {
                try {
                    redisTemplate.opsForValue().set(REDIS_PAGERANK_PREFIX + entry.getKey(), score);
                } catch (Exception e) {
                    redisAvailable = false;
                }
            }
        }

        log.info("Computed PageRank across {} pages in {} iterations. Max authority score: {}",
                N, iteration, maxRank);

        return new HashMap<>(inMemoryPageRank);
    }

    public double getPageRank(String url) {
        if (url == null) return 1.0;

        if (redisAvailable) {
            try {
                Object val = redisTemplate.opsForValue().get(REDIS_PAGERANK_PREFIX + url);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }

        return inMemoryPageRank.getOrDefault(url, 1.0);
    }

    public Map<String, Double> getAllPageRanks() {
        return Collections.unmodifiableMap(inMemoryPageRank);
    }

    public void clear() {
        if (redisAvailable) {
            try {
                for (String url : inMemoryPageRank.keySet()) {
                    redisTemplate.delete(REDIS_PAGERANK_PREFIX + url);
                }
            } catch (Exception e) {
                redisAvailable = false;
            }
        }
        inMemoryPageRank.clear();
    }
}
