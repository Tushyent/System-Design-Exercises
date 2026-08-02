package com.consistenthashing.service;

import com.consistenthashing.consistenthash.ConsistentHashRing;
import com.consistenthashing.consistenthash.StorageNode;
import com.consistenthashing.model.Student;
import com.consistenthashing.storage.StudentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private static final String MONGO4_DISPLAY = "mongo4:27020";

    private final ConsistentHashRing ring;
    private final StudentStore store;
    private final String mongo4Host;
    private final int mongo4Port;

    public NodeService(ConsistentHashRing ring, StudentStore store,
                       @Value("${app.mongo4.host:localhost}") String mongo4Host,
                       @Value("${app.mongo4.port:27020}") int mongo4Port) {
        this.ring = ring;
        this.store = store;
        this.mongo4Host = mongo4Host;
        this.mongo4Port = mongo4Port;
    }

    public record NodeChange(String nodeKey, int migrated, Map<String, Long> before, Map<String, Long> after) {
    }

    public List<StorageNode> listNodes() {
        return ring.getPhysicalNodes();
    }

    public Map<String, Integer> vnodeCounts() {
        return ring.vnodeCounts();
    }

    private Map<String, Long> renameKey(Map<String, Long> map, String oldKey, String newKey) {
        Map<String, Long> renamed = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (entry.getKey().equals(oldKey)) {
                renamed.put(newKey, entry.getValue());
            } else {
                renamed.put(entry.getKey(), entry.getValue());
            }
        }
        return renamed;
    }

    private StorageNode resolveNode(String host, int port) {
        if ("mongo4".equalsIgnoreCase(host)) {
            return new StorageNode(mongo4Host, mongo4Port);
        }
        return new StorageNode(host, port);
    }

    public NodeChange addNode(String host, int port) {
        boolean isMongo4 = "mongo4".equalsIgnoreCase(host);
        StorageNode node = resolveNode(host, port);
        if (ring.getPhysicalNodes().contains(node)) {
            throw new IllegalArgumentException("node already registered: " + node.key());
        }
        Map<String, Long> before = snapshot();
        ring.addNode(node);
        log.info("Added node {} to the hash ring", node.key());

        int migrated = migrateOffExistingNodes(List.copyOf(ring.getPhysicalNodes()), node);
        Map<String, Long> after = snapshot();
        log.info("Add-node migration complete: {} record(s) moved to {}", migrated, node.key());

        if (isMongo4) {
            before = renameKey(before, node.key(), MONGO4_DISPLAY);
            after = renameKey(after, node.key(), MONGO4_DISPLAY);
            return new NodeChange(MONGO4_DISPLAY, migrated, before, after);
        }
        return new NodeChange(node.key(), migrated, before, after);
    }

    public NodeChange removeNode(String host, int port) {
        boolean isMongo4 = "mongo4".equalsIgnoreCase(host);
        StorageNode node = resolveNode(host, port);
        Map<String, Long> before = snapshot();

        List<Student> stranded = store.allStudentsOn(node);
        ring.removeNode(node);
        log.info("Removed node {} from the hash ring; relocating {} record(s)", node.key(), stranded.size());

        int migrated = 0;
        for (Student s : stranded) {
            StorageNode newOwner = ring.getNode(s.getRollNo().toString());
            store.insertOn(s, newOwner);
            migrated++;
        }
        store.dropOn(node);

        Map<String, Long> after = snapshot();
        log.info("Remove-node migration complete: {} record(s) relocated", migrated);

        if (isMongo4) {
            before = renameKey(before, node.key(), MONGO4_DISPLAY);
            after = renameKey(after, node.key(), MONGO4_DISPLAY);
            return new NodeChange(MONGO4_DISPLAY, migrated, before, after);
        }
        return new NodeChange(node.key(), migrated, before, after);
    }

    private int migrateOffExistingNodes(List<StorageNode> nodes, StorageNode newNode) {
        int migrated = 0;
        for (StorageNode existing : nodes) {
            if (existing.equals(newNode)) {
                continue;
            }
            for (Student s : store.allStudentsOn(existing)) {
                StorageNode newOwner = ring.getNode(s.getRollNo().toString());
                if (!newOwner.equals(existing)) {
                    store.insertOn(s, newOwner);
                    store.deleteOn(s.getRollNo(), existing);
                    migrated++;
                }
            }
        }
        return migrated;
    }

    private Map<String, Long> snapshot() {
        Map<String, Long> snap = new LinkedHashMap<>();
        for (StorageNode n : ring.getPhysicalNodes()) {
            String key = n.key();
            snap.put(key, store.countOn(n));
        }
        return snap;
    }
}
