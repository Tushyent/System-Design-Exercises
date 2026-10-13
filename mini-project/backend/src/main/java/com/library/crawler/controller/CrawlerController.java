package com.library.crawler.controller;

import com.library.common.dto.ApiResponse;
import com.library.crawler.model.CrawlerJob;
import com.library.crawler.service.WebCrawlerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final WebCrawlerService crawlerService;

    @Data
    public static class TriggerCrawlRequest {
        private String sourceUrl;
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CrawlerJob>> triggerCrawl(
            @RequestBody(required = false) TriggerCrawlRequest request) {

        String source = (request != null && request.getSourceUrl() != null && !request.getSourceUrl().isBlank())
                ? request.getSourceUrl()
                : "http://localhost:8080/mock-external-library/books";

        CrawlerJob job = crawlerService.startCrawlJob(source);
        return ResponseEntity.ok(ApiResponse.ok(job, "Crawl job initiated"));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CrawlerJob>>> getCrawlJobs() {
        List<CrawlerJob> jobs = crawlerService.getAllJobs();
        return ResponseEntity.ok(ApiResponse.ok(jobs, "Crawl jobs history retrieved"));
    }
}
