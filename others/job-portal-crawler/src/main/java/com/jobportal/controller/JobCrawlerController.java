package com.jobportal.controller;

import com.jobportal.dto.CrawlRequest;
import com.jobportal.dto.CrawlResponse;
import com.jobportal.ratelimit.BucketStatus;
import com.jobportal.ratelimit.Decision;
import com.jobportal.service.CrawlerService;
import com.jobportal.service.RateLimiterService;
import com.jobportal.service.UrlQueueService;
import com.jobportal.service.VisitedUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Question (a) + (b): rate-limited job-listing crawler API.
 *
 * <p>Every {@code POST /api/crawl} first passes the Redis token bucket
 * (capacity 5, refill 1/s, per client). When a token exists it is consumed
 * and the crawl runs; otherwise the API rejects with HTTP 429.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Job Portal Crawler API", description = "Job discovery crawl + queue/visited inspection + bucket management")
public class JobCrawlerController {

    private final RateLimiterService rateLimiter;
    private final CrawlerService crawler;
    private final UrlQueueService urlQueue;
    private final VisitedUrlService visited;

    @PostMapping("/crawl")
    @Operation(summary = "Crawl job listings from a seed URL (max 10 pages, rate-limited)",
            description = "Accepts { seedUrl }. Consumes 1 token from the client's bucket "
                    + "(capacity 5, refill 1/s). Returns crawled pages, discovered URLs, "
                    + "visited URLs and queue remainder. HTTP 429 when the bucket is empty.")
    public ResponseEntity<CrawlResponse> crawl(@Valid @RequestBody CrawlRequest request,
                                               HttpServletRequest http) {
        String clientId = rateLimiter.resolveClientId(http);
        // clientId may also be supplied inside the JSON body for Postman convenience
        Decision decision = rateLimiter.check(clientId);
        CrawlResponse response = crawler.crawl(request.seedUrl(), clientId, decision.remainingTokens());
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf((long) Math.floor(decision.remainingTokens())))
                .body(response);
    }

    @GetMapping("/queue")
    @Operation(summary = "Peek at the URL queue (FIFO frontier)")
    public ResponseEntity<Map<String, Object>> queue(@RequestParam(defaultValue = "50") int limit) {
        List<String> urls = urlQueue.peekAll(Math.max(1, Math.min(limit, 200)));
        Map<String, Object> out = new HashMap<>();
        out.put("queueSize", urlQueue.size());
        out.put("storage", urlQueue.isUsingRedis() ? "REDIS_LIST" : "IN_MEMORY_QUEUE");
        out.put("dataStructure", "Queue<String> FIFO (RPUSH/LPOP)");
        out.put("pendingUrls", urls);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/visited")
    @Operation(summary = "List the visited-URL set (duplicate guard)")
    public ResponseEntity<Map<String, Object>> visited() {
        Map<String, Object> out = new HashMap<>();
        out.put("visitedCount", visited.getVisitedCount());
        out.put("storage", visited.isUsingRedis() ? "REDIS_SET" : "IN_MEMORY_SET");
        out.put("dataStructure", "Set<String> (SADD/SISMEMBER)");
        out.put("visitedUrls", visited.getAllVisited());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/bucket/{clientId}")
    @Operation(summary = "Read-only view of a client's token bucket (no token consumed)")
    public BucketStatus bucket(@PathVariable String clientId) {
        return rateLimiter.status(clientId);
    }

    @DeleteMapping("/bucket/{clientId}")
    @Operation(summary = "Reset a client's bucket to full (replay the demo)")
    public ResponseEntity<Map<String, String>> resetBucket(@PathVariable String clientId) {
        rateLimiter.reset(clientId);
        return ResponseEntity.ok(Map.of("clientId", clientId, "status", "RESET_TO_FULL"));
    }

    @PostMapping("/reset")
    @Operation(summary = "Clear crawler state (URL queue + visited set)")
    public ResponseEntity<Map<String, String>> resetCrawler() {
        crawler.reset();
        return ResponseEntity.ok(Map.of("status", "CRAWLER_STATE_CLEARED"));
    }

    @GetMapping("/mock-urls")
    @Operation(summary = "Seed URLs: mock site + real company career pages")
    public ResponseEntity<List<Map<String, String>>> mockUrls(HttpServletRequest http) {
        String base = "http://localhost:8080";
        return ResponseEntity.ok(List.of(
                Map.of("name", "Mock Jobs Home (listing hub)", "url", base + "/mock/jobs",
                        "type", "Seed: offline-safe, 6 postings + 2 categories"),
                Map.of("name", "Mock Engineering Category", "url", base + "/mock/jobs/category/engineering",
                        "type", "Seed: overlap with hub (dedup demo)"),
                Map.of("name", "Google Careers", "url", "https://www.google.com/about/careers/applications/jobs/results/",
                        "type", "Real: may need internet; JS board"),
                Map.of("name", "Microsoft Jobs", "url", "https://jobs.career.microsoft.com/global/en/search",
                        "type", "Real: may need internet; JS board"),
                Map.of("name", "Amazon Jobs", "url", "https://www.amazon.jobs/en/search?base_query=engineer",
                        "type", "Real: server-rendered, usually crawlable"),
                Map.of("name", "Zoho Careers", "url", "https://careers.zohocorp.com/jobs/",
                        "type", "Real: server-rendered, usually crawlable"),
                Map.of("name", "Netflix Jobs", "url", "https://jobs.netflix.net/search",
                        "type", "Real: may need internet; JS board"),
                Map.of("name", "TCS Careers", "url", "https://www.tcs.com/careers",
                        "type", "Real: corporate site, usually crawlable")));
    }
}
