package com.library.consistenthash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsistentHashRingTest {

    @Test
    @DisplayName("Should route identical keys to the same node deterministically")
    void testDeterministicRouting() {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(50);
        ring.addNode("server-1");
        ring.addNode("server-2");
        ring.addNode("server-3");

        String nodeA = ring.getNode("book-isbn-9781449373320");
        String nodeB = ring.getNode("book-isbn-9781449373320");

        assertNotNull(nodeA);
        assertEquals(nodeA, nodeB, "Same key must deterministically route to the same node");
    }

    @Test
    @DisplayName("Should remap minority of keys upon adding a new node")
    void testRemappingOnNodeAddition() {
        ConsistentHashService service = new ConsistentHashService();
        Map<String, Object> benchmark = service.runDistributionBenchmark(
                2000,
                100,
                List.of("node-1", "node-2", "node-3")
        );

        double chRemappedPct = (double) benchmark.get("consistentHashingRemappingPercentage");
        double modRemappedPct = (double) benchmark.get("moduloHashingRemappingPercentage");

        // When going from 3 to 4 nodes:
        // Modulo hashing remaps ~75% of keys!
        // Consistent hashing remaps only ~25% of keys!
        assertTrue(chRemappedPct < 35.0, "Consistent hashing remapping should be close to 25% (observed: " + chRemappedPct + "%)");
        assertTrue(modRemappedPct > 65.0, "Modulo hashing should remap majority (~75%) of keys (observed: " + modRemappedPct + "%)");
        assertTrue(chRemappedPct < modRemappedPct, "Consistent hashing should remap significantly fewer keys than modulo hashing");
    }
}
