package com.library.consistenthash;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent Hash Ring supporting virtual nodes.
 * Maps keys uniformly along a 32-bit integer ring [0, 2^32 - 1].
 */
public class ConsistentHashRing<T> {

    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new ConcurrentSkipListMap<>();
    private final Set<T> physicalNodes = Collections.synchronizedSet(new HashSet<>());

    public ConsistentHashRing(int numberOfReplicas) {
        this.numberOfReplicas = Math.max(1, numberOfReplicas);
    }

    public synchronized void addNode(T node) {
        if (physicalNodes.add(node)) {
            for (int i = 0; i < numberOfReplicas; i++) {
                long hash = HashFunction.hash(node.toString() + "-vnode-" + i);
                circle.put(hash, node);
            }
        }
    }

    public synchronized void removeNode(T node) {
        if (physicalNodes.remove(node)) {
            for (int i = 0; i < numberOfReplicas; i++) {
                long hash = HashFunction.hash(node.toString() + "-vnode-" + i);
                circle.remove(hash);
            }
        }
    }

    public T getNode(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = HashFunction.hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public Set<T> getPhysicalNodes() {
        return Collections.unmodifiableSet(physicalNodes);
    }

    public int getVirtualNodesCount() {
        return circle.size();
    }

    public Map<T, Integer> calculateKeyDistribution(List<String> keys) {
        Map<T, Integer> distribution = new HashMap<>();
        for (T node : physicalNodes) {
            distribution.put(node, 0);
        }
        for (String key : keys) {
            T node = getNode(key);
            if (node != null) {
                distribution.put(node, distribution.getOrDefault(node, 0) + 1);
            }
        }
        return distribution;
    }
}
