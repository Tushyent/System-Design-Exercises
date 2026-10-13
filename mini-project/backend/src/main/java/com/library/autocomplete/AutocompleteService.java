package com.library.autocomplete;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutocompleteService {

    private final PrefixTrie prefixTrie;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final AtomicLong queryCount = new AtomicLong(0);
    private final AtomicLong totalLatencyMicros = new AtomicLong(0);

    public List<AutocompleteSuggestion> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isBlank()) {
            return Collections.emptyList();
        }

        long startNanos = System.nanoTime();
        String normalized = prefix.trim().toLowerCase(Locale.ROOT);
        String cacheKey = "autocomplete:" + normalized + ":" + limit;

        List<AutocompleteSuggestion> results = null;

        // 1. Check Redis Cache
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null) {
                    results = objectMapper.readValue(cachedJson, new TypeReference<List<AutocompleteSuggestion>>() {});
                }
            } catch (Exception e) {
                log.warn("Redis autocomplete cache read failed: {}", e.getMessage());
            }
        }

        // 2. Fallback to In-Memory Trie
        if (results == null) {
            results = prefixTrie.search(normalized, limit);

            // Populate Redis Cache (5 min TTL)
            if (redisTemplate != null && !results.isEmpty()) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(results), Duration.ofMinutes(5));
                } catch (Exception e) {
                    log.warn("Redis autocomplete cache write failed: {}", e.getMessage());
                }
            }
        }

        long elapsedMicros = (System.nanoTime() - startNanos) / 1000;
        queryCount.incrementAndGet();
        totalLatencyMicros.addAndGet(elapsedMicros);

        return results;
    }

    public Map<String, Object> evaluateAccuracy(int k) {
        // Labelled test ground truth dataset: query prefix -> expected top relevant keywords
        Map<String, Set<String>> testGroundTruth = Map.of(
                "data", Set.of("Designing Data-Intensive Applications", "Database System Concepts"),
                "clean", Set.of("Clean Code", "Clean Architecture"),
                "dist", Set.of("Distributed Systems", "Designing Data-Intensive Applications"),
                "algo", Set.of("Introduction to Algorithms"),
                "comp", Set.of("Computer Networks", "Computer Architecture")
        );

        int totalQueries = testGroundTruth.size();
        double sumPrecision = 0.0;
        double sumRecall = 0.0;

        List<Map<String, Object>> queryEvaluations = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : testGroundTruth.entrySet()) {
            String prefix = entry.getKey();
            Set<String> expected = entry.getValue();

            List<AutocompleteSuggestion> suggestions = getSuggestions(prefix, k);
            Set<String> predicted = new HashSet<>();
            for (AutocompleteSuggestion s : suggestions) {
                predicted.add(s.getText());
            }

            // Calculate intersection
            int relevantRetrieved = 0;
            for (String p : predicted) {
                for (String exp : expected) {
                    if (p.toLowerCase().contains(exp.toLowerCase()) || exp.toLowerCase().contains(p.toLowerCase())) {
                        relevantRetrieved++;
                        break;
                    }
                }
            }

            double precision = suggestions.isEmpty() ? 0.0 : (double) relevantRetrieved / suggestions.size();
            double recall = expected.isEmpty() ? 0.0 : (double) relevantRetrieved / expected.size();

            sumPrecision += precision;
            sumRecall += recall;

            queryEvaluations.add(Map.of(
                    "prefix", prefix,
                    "expectedCount", expected.size(),
                    "retrievedCount", suggestions.size(),
                    "relevantRetrieved", relevantRetrieved,
                    "precisionAtK", precision,
                    "recallAtK", recall
            ));
        }

        double meanPrecisionAtK = sumPrecision / totalQueries;
        double meanRecallAtK = sumRecall / totalQueries;
        double avgLatencyMs = queryCount.get() > 0 ? (totalLatencyMicros.get() / (double) queryCount.get()) / 1000.0 : 0.0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("k", k);
        report.put("totalEvaluationQueries", totalQueries);
        report.put("meanPrecisionAtK", meanPrecisionAtK);
        report.put("meanRecallAtK", meanRecallAtK);
        report.put("averageLatencyMs", avgLatencyMs);
        report.put("totalQueriesRun", queryCount.get());
        report.put("queries", queryEvaluations);

        return report;
    }

    public Map<String, Object> getMetrics() {
        long count = queryCount.get();
        double avgMs = count > 0 ? (totalLatencyMicros.get() / (double) count) / 1000.0 : 0.0;
        return Map.of(
                "totalQueries", count,
                "averageLatencyMs", avgMs
        );
    }
}
