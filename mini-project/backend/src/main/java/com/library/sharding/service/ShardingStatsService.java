package com.library.sharding.service;

import com.library.consistenthash.HashFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShardingStatsService {

    private final MongoTemplate mongoTemplate;

    public Map<String, Object> getShardingStatus() {
        Map<String, Object> response = new LinkedHashMap<>();

        // Check if current MongoDB instance is a mongos router
        boolean isNativeSharded = false;
        try {
            Document serverStatus = mongoTemplate.executeCommand(new Document("serverStatus", 1));
            String process = serverStatus.getString("process");
            if ("mongos".equalsIgnoreCase(process)) {
                isNativeSharded = true;
            }
        } catch (Exception e) {
            log.warn("Could not determine mongos process status: {}", e.getMessage());
        }

        response.put("isNativeShardedCluster", isNativeSharded);
        response.put("databaseName", mongoTemplate.getDb().getName());

        // Defined Shard Keys in Schema
        Map<String, Object> shardKeys = Map.of(
                "books", Map.of("shardKey", "{ \"_id\": \"hashed\" }", "strategy", "Hashed Sharding for uniform write/read distribution"),
                "members", Map.of("shardKey", "{ \"memberId\": \"hashed\" }", "strategy", "Hashed Sharding for balanced member partitions"),
                "loans", Map.of("shardKey", "{ \"memberId\": 1, \"loanId\": 1 }", "strategy", "Compound Range Sharding for member loan query locality")
        );
        response.put("configuredShardKeys", shardKeys);

        // Record Counts per Collection
        List<String> collections = List.of("books", "book_copies", "members", "loans", "reservations", "fines", "payments");
        Map<String, Long> collectionCounts = new HashMap<>();
        for (String col : collections) {
            try {
                long count = mongoTemplate.getCollection(col).countDocuments();
                collectionCounts.put(col, count);
            } catch (Exception e) {
                collectionCounts.put(col, 0L);
            }
        }
        response.put("collectionCounts", collectionCounts);

        // Partition Distribution Breakdown across 2 Shards
        Map<String, Object> partitionDistribution = calculateSimulatedDistribution();
        response.put("shardDistributionAnalysis", partitionDistribution);

        return response;
    }

    private Map<String, Object> calculateSimulatedDistribution() {
        // Evaluate hash distribution across 2 logical shards:
        // Shard-1: hash in [0, 2^31 - 1]
        // Shard-2: hash in [2^31, 2^32 - 1]
        long midPoint = 2147483648L; // 2^31

        int shard1Books = 0;
        int shard2Books = 0;

        List<Document> books = mongoTemplate.getCollection("books").find().into(new ArrayList<>());
        for (Document doc : books) {
            String id = doc.getString("_id");
            if (id != null) {
                long h = HashFunction.hash(id);
                if (h < midPoint) {
                    shard1Books++;
                } else {
                    shard2Books++;
                }
            }
        }

        int shard1Members = 0;
        int shard2Members = 0;
        List<Document> members = mongoTemplate.getCollection("members").find().into(new ArrayList<>());
        for (Document doc : members) {
            String memberId = doc.getString("memberId");
            if (memberId != null) {
                long h = HashFunction.hash(memberId);
                if (h < midPoint) {
                    shard1Members++;
                } else {
                    shard2Members++;
                }
            }
        }

        int totalBooks = books.size();
        int totalMembers = members.size();

        return Map.of(
                "shard1", Map.of(
                        "name", "shard-rs-01",
                        "booksCount", shard1Books,
                        "booksPercent", totalBooks > 0 ? ((double) shard1Books / totalBooks) * 100.0 : 50.0,
                        "membersCount", shard1Members,
                        "membersPercent", totalMembers > 0 ? ((double) shard1Members / totalMembers) * 100.0 : 50.0
                ),
                "shard2", Map.of(
                        "name", "shard-rs-02",
                        "booksCount", shard2Books,
                        "booksPercent", totalBooks > 0 ? ((double) shard2Books / totalBooks) * 100.0 : 50.0,
                        "membersCount", shard2Members,
                        "membersPercent", totalMembers > 0 ? ((double) shard2Members / totalMembers) * 100.0 : 50.0
                )
        );
    }
}
