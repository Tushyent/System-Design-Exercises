package com.library.sharding.controller;

import com.library.common.dto.ApiResponse;
import com.library.sharding.service.ShardingStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sharding")
@RequiredArgsConstructor
public class ShardingController {

    private final ShardingStatsService shardingStatsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShardingStats() {
        Map<String, Object> status = shardingStatsService.getShardingStatus();
        return ResponseEntity.ok(ApiResponse.ok(status, "Sharding and partition metrics retrieved"));
    }
}
