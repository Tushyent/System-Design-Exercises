package com.consistenthashing.service;

import com.consistenthashing.consistenthash.ConsistentHashRing;
import com.consistenthashing.consistenthash.StorageNode;
import com.consistenthashing.dto.DistributionReport;
import com.consistenthashing.storage.StudentStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DistributionService {

    private static final String MONGO4_DISPLAY = "mongo4:27020";

    private final ConsistentHashRing ring;
    private final StudentStore store;
    private final String mongo4Host;
    private final int mongo4Port;

    public DistributionService(ConsistentHashRing ring, StudentStore store,
                               @Value("${app.mongo4.host:localhost}") String mongo4Host,
                               @Value("${app.mongo4.port:27020}") int mongo4Port) {
        this.ring = ring;
        this.store = store;
        this.mongo4Host = mongo4Host;
        this.mongo4Port = mongo4Port;
    }

    private String displayKey(String key) {
        if ((mongo4Host + ":" + mongo4Port).equals(key)) {
            return MONGO4_DISPLAY;
        }
        return key;
    }

    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (StorageNode node : ring.getPhysicalNodes()) {
            counts.put(displayKey(node.key()), store.countOn(node));
        }
        return counts;
    }

    public DistributionReport report() {
        return report(Map.of());
    }

    public DistributionReport report(Map<String, Long> before) {
        Map<String, Long> after = counts();
        long total = after.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Long> vnodes = new LinkedHashMap<>();
        ring.vnodeCounts().forEach((k, v) -> vnodes.put(displayKey(k), v.longValue()));

        Map<String, Long> beforeMapped = new LinkedHashMap<>();
        before.forEach((k, v) -> beforeMapped.put(displayKey(k), v));

        Map<String, Long> delta = new LinkedHashMap<>();
        for (String k : after.keySet()) {
            delta.put(k, after.get(k) - beforeMapped.getOrDefault(k, 0L));
        }
        return new DistributionReport(after, vnodes, total, delta);
    }
}
