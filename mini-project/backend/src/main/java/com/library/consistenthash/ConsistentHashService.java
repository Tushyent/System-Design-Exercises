package com.library.consistenthash;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConsistentHashService {

    private final ConsistentHashRing<String> serverRing = new ConsistentHashRing<>(100);

    public ConsistentHashService() {
        // Initialize with default library service nodes
        serverRing.addNode("library-service-node-1");
        serverRing.addNode("library-service-node-2");
        serverRing.addNode("library-service-node-3");
    }

    public synchronized void addNode(String nodeName) {
        serverRing.addNode(nodeName);
    }

    public synchronized void removeNode(String nodeName) {
        serverRing.removeNode(nodeName);
    }

    public String routeKey(String key) {
        return serverRing.getNode(key);
    }

    public Map<String, Object> getRingState() {
        Map<String, Object> state = new HashMap<>();
        state.put("nodes", serverRing.getPhysicalNodes());
        state.put("nodeCount", serverRing.getPhysicalNodes().size());
        state.put("virtualNodesCount", serverRing.getVirtualNodesCount());
        return state;
    }

    /**
     * Simulates load balancing and compares Consistent Hashing with Modulo Hashing
     */
    public Map<String, Object> runDistributionBenchmark(int keyCount, int vnodes, List<String> initialNodes) {
        ConsistentHashRing<String> testRing = new ConsistentHashRing<>(vnodes);
        for (String node : initialNodes) {
            testRing.addNode(node);
        }

        List<String> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add("book-key-" + (100000 + i));
        }

        // 1. Consistent Hashing Initial Distribution
        Map<String, Integer> chInitialDist = testRing.calculateKeyDistribution(keys);

        // 2. Modulo Hashing Initial Distribution
        Map<String, Integer> modInitialDist = new HashMap<>();
        for (String node : initialNodes) {
            modInitialDist.put(node, 0);
        }
        for (String key : keys) {
            int nodeIndex = Math.abs(key.hashCode()) % initialNodes.size();
            String target = initialNodes.get(nodeIndex);
            modInitialDist.put(target, modInitialDist.get(target) + 1);
        }

        // 3. Node Addition Experiment (Add 1 node)
        String newNode = "library-service-node-" + (initialNodes.size() + 1);
        List<String> expandedNodes = new ArrayList<>(initialNodes);
        expandedNodes.add(newNode);

        // Calculate remapping for Consistent Hashing
        Map<String, String> chInitialMapping = new HashMap<>();
        for (String key : keys) {
            chInitialMapping.put(key, testRing.getNode(key));
        }

        testRing.addNode(newNode);
        int chRemappedCount = 0;
        for (String key : keys) {
            if (!Objects.equals(chInitialMapping.get(key), testRing.getNode(key))) {
                chRemappedCount++;
            }
        }
        double chRemappedPct = ((double) chRemappedCount / keyCount) * 100.0;
        Map<String, Integer> chExpandedDist = testRing.calculateKeyDistribution(keys);

        // Calculate remapping for Modulo Hashing
        int modRemappedCount = 0;
        for (String key : keys) {
            int oldIdx = Math.abs(key.hashCode()) % initialNodes.size();
            int newIdx = Math.abs(key.hashCode()) % expandedNodes.size();
            if (oldIdx != newIdx) {
                modRemappedCount++;
            }
        }
        double modRemappedPct = ((double) modRemappedCount / keyCount) * 100.0;

        // Compile comprehensive report
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyCount", keyCount);
        result.put("virtualNodesPerPhysicalNode", vnodes);
        result.put("initialNodeCount", initialNodes.size());
        result.put("expandedNodeCount", expandedNodes.size());

        // Standard Deviation / Imbalance of Consistent Hashing
        double chStdDev = calculateStdDev(chInitialDist.values());
        double modStdDev = calculateStdDev(modInitialDist.values());

        result.put("consistentHashingInitialDistribution", chInitialDist);
        result.put("consistentHashingStdDev", chStdDev);
        result.put("consistentHashingExpandedDistribution", chExpandedDist);
        result.put("consistentHashingKeysRemapped", chRemappedCount);
        result.put("consistentHashingRemappingPercentage", chRemappedPct);

        result.put("moduloHashingInitialDistribution", modInitialDist);
        result.put("moduloHashingStdDev", modStdDev);
        result.put("moduloHashingKeysRemapped", modRemappedCount);
        result.put("moduloHashingRemappingPercentage", modRemappedPct);

        result.put("theoreticalExpectedChRemapPct", (1.0 / expandedNodes.size()) * 100.0);
        result.put("theoreticalExpectedModRemapPct", ((double) initialNodes.size() / expandedNodes.size()) * 100.0);

        return result;
    }

    private double calculateStdDev(Collection<Integer> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (int v : values) sum += v;
        double mean = sum / values.size();
        double var = 0.0;
        for (int v : values) {
            var += Math.pow(v - mean, 2);
        }
        return Math.sqrt(var / values.size());
    }
}
