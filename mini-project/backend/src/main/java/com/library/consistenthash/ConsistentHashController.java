package com.library.consistenthash;

import com.library.common.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consistenthash")
@RequiredArgsConstructor
public class ConsistentHashController {

    private final ConsistentHashService hashService;

    @GetMapping("/ring")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRingState() {
        return ResponseEntity.ok(ApiResponse.ok(hashService.getRingState(), "Consistent hash ring state"));
    }

    @GetMapping("/route")
    public ResponseEntity<ApiResponse<Map<String, String>>> routeKey(@RequestParam String key) {
        String targetNode = hashService.routeKey(key);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("key", key, "routedNode", targetNode),
                "Key routed successfully"
        ));
    }

    @PostMapping("/nodes")
    public ResponseEntity<ApiResponse<String>> addNode(@RequestParam String node) {
        hashService.addNode(node);
        return ResponseEntity.ok(ApiResponse.ok(node, "Node added to consistent hash ring"));
    }

    @DeleteMapping("/nodes")
    public ResponseEntity<ApiResponse<String>> removeNode(@RequestParam String node) {
        hashService.removeNode(node);
        return ResponseEntity.ok(ApiResponse.ok(node, "Node removed from consistent hash ring"));
    }

    @Data
    public static class SimulationRequest {
        private int keyCount = 1000;
        private int virtualNodes = 100;
        private List<String> initialNodes = List.of(
                "library-server-node-1",
                "library-server-node-2",
                "library-server-node-3"
        );
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runSimulation(
            @RequestBody(required = false) SimulationRequest request) {

        if (request == null) {
            request = new SimulationRequest();
        }

        Map<String, Object> results = hashService.runDistributionBenchmark(
                request.getKeyCount(),
                request.getVirtualNodes(),
                request.getInitialNodes()
        );

        return ResponseEntity.ok(ApiResponse.ok(results, "Consistent Hashing vs Modulo Hashing benchmark completed"));
    }
}
