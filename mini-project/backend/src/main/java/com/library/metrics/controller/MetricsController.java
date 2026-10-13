package com.library.metrics.controller;

import com.library.common.dto.ApiResponse;
import com.library.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetricsSummary() {
        Map<String, Object> metrics = metricsService.getSystemMetricsSummary();
        return ResponseEntity.ok(ApiResponse.ok(metrics, "Real-time system design metrics summary"));
    }
}
