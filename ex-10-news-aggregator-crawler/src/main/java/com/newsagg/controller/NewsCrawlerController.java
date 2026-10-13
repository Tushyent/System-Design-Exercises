package com.newsagg.controller;

import com.newsagg.dto.CrawlRequest;
import com.newsagg.dto.CrawlResponse;
import com.newsagg.ratelimit.BucketStatus;
import com.newsagg.ratelimit.Decision;
import com.newsagg.service.CrawlerService;
import com.newsagg.service.RateLimiterService;
import com.newsagg.service.UrlQueueService;
import com.newsagg.service.VisitedUrlService;
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
 * Question (a) + (b): rate-limited news crawler API.
 *
 * <p>Every {@code POST /api/crawl} first passes the Redis token bucket
 * (capacity 5, refill 1/s, per client). When a token exists it is consumed
 * and the crawl runs; otherwise the API rejects with HTTP 429.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "News Aggregator Crawler API", description = "News discovery crawl + queue/visited inspection + bucket management")
public class NewsCrawlerController {

    private final RateLimiterService rateLimiter;
    private final CrawlerService crawler;
    private final UrlQueueService urlQueue;
    private final VisitedUrlService visited;

    @PostMapping("/crawl")
    @Operation(summary = "Crawl news articles from a seed URL (max 10 pages, rate-limited)",
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
    @Operation(summary = "Seed URLs: mock news site + real news websites")
    public ResponseEntity<List<Map<String, String>>> mockUrls(HttpServletRequest http) {
        String base = "http://localhost:8081";
        return ResponseEntity.ok(List.of(
                Map.of("name", "Mock Front Page (top stories)", "url", base + "/mock/news",
                        "type", "Seed: offline-safe, 6 articles + 2 sections"),
                Map.of("name", "Mock Technology Section", "url", base + "/mock/news/section/technology",
                        "type", "Seed: overlap with front page (dedup demo)"),
                Map.of("name", "Mock Sports Section", "url", base + "/mock/news/section/sports",
                        "type", "Seed: overlap with front page (dedup demo)"),
                Map.of("name", "BBC News", "url", "https://www.bbc.com/news",
                        "type", "Real: server-rendered, usually crawlable"),
                Map.of("name", "Hacker News", "url", "https://news.ycombinator.com/",
                        "type", "Real: plain HTML, very crawlable"),
                Map.of("name", "The Hindu", "url", "https://www.thehindu.com/",
                        "type", "Real: server-rendered, usually crawlable"),
                Map.of("name", "NDTV", "url", "https://www.ndtv.com/",
                        "type", "Real: server-rendered, usually crawlable"),
                Map.of("name", "Reuters", "url", "https://www.reuters.com/",
                        "type", "Real: may need internet; JS-heavy")));
    }
}
