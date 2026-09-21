package com.crawler.controller;

import com.crawler.dto.CrawlRequest;
import com.crawler.dto.SearchResponse;
import com.crawler.dto.SingleCrawlResponse;
import com.crawler.model.CrawlStats;
import com.crawler.model.CrawlTask;
import com.crawler.model.DiscoveredWebsite;
import com.crawler.model.PageMetadata;
import com.crawler.service.UrlFrontierQueue;
import com.crawler.service.VisitedUrlService;
import com.crawler.service.WebCrawlerEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/crawler")
@CrossOrigin(origins = "*")
@Tag(name = "Web Crawler API", description = "Endpoints for orchestrating web crawling, full-text content discovery, and inspecting Redis cache")
public class CrawlerController {

    private final WebCrawlerEngine crawlerEngine;
    private final UrlFrontierQueue frontierQueue;
    private final VisitedUrlService visitedUrlService;

    public CrawlerController(WebCrawlerEngine crawlerEngine,
                             UrlFrontierQueue frontierQueue,
                             VisitedUrlService visitedUrlService) {
        this.crawlerEngine = crawlerEngine;
        this.frontierQueue = frontierQueue;
        this.visitedUrlService = visitedUrlService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start Crawling Job", description = "Initiates asynchronous crawling from seed URLs using configured limits, workers, robots.txt compliance, and sitemap discovery")
    public ResponseEntity<CrawlStats> startCrawl(@RequestBody(required = false) CrawlRequest request) {
        CrawlStats stats = crawlerEngine.startCrawl(request);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop Crawling Job", description = "Terminates the active crawling session and halts all worker threads")
    public ResponseEntity<CrawlStats> stopCrawl() {
        CrawlStats stats = crawlerEngine.stopCrawl();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset Crawler State", description = "Clears the URL queue, visited set, cached pages, search index, and resets performance metrics")
    public ResponseEntity<Map<String, Object>> resetCrawler() {
        crawlerEngine.resetState();
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Web crawler state, search index, and Redis storage successfully reset.");
        resp.put("status", "SUCCESS");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/crawl-single")
    @Operation(summary = "Crawl Single URL Synchronously", description = "Fetches a single URL, extracts clean content, metadata, and links. Checks Redis cache first for sub-ms latency.")
    public ResponseEntity<SingleCrawlResponse> crawlSingle(@RequestParam String url) {
        SingleCrawlResponse response = crawlerEngine.crawlSingleUrl(url);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Full-Text Content Discovery & Ranking Search", description = "Searches discovered web content using inverted indexing, BM25 ranking, Google PageRank, and topic clustering")
    public ResponseEntity<SearchResponse> searchContent(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "COMPOSITE") String sortBy,
            @RequestParam(required = false) String category) {
        SearchResponse response = crawlerEngine.searchContent(q, sortBy, category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Vocabulary Autocomplete Suggestions", description = "Returns matching keyword suggestions from the crawled vocabulary as the user types")
    public ResponseEntity<List<Map<String, Object>>> getAutocomplete(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "8") int limit) {
        List<Map<String, Object>> suggestions = crawlerEngine.getAutocomplete(prefix, limit);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/pagerank")
    @Operation(summary = "Get PageRank Authority Distribution", description = "Returns computed Google PageRank authority scores across all discovered pages")
    public ResponseEntity<Map<String, Object>> getPageRank() {
        Map<String, Double> ranks = crawlerEngine.getAllPageRanks();
        if (ranks.isEmpty()) {
            ranks = crawlerEngine.computePageRank();
        }

        List<Map<String, Object>> leaderboard = ranks.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("url", e.getKey());
                    item.put("pageRankScore", e.getValue());
                    PageMetadata pm = crawlerEngine.getPage(e.getKey());
                    item.put("title", pm != null ? pm.getTitle() : "N/A");
                    item.put("category", pm != null ? pm.getCategory() : "General");
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalPages", ranks.size());
        resp.put("dampingFactor", 0.85);
        resp.put("leaderboard", leaderboard);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/pagerank/recompute")
    @Operation(summary = "Recompute PageRank", description = "Re-executes the PageRank power iteration algorithm across all crawled pages")
    public ResponseEntity<Map<String, Object>> recomputePageRank() {
        Map<String, Double> ranks = crawlerEngine.computePageRank();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalPagesScored", ranks.size());
        resp.put("ranks", ranks);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/topics")
    @Operation(summary = "Discovered Topics Taxonomy", description = "Returns categorization distribution and extracted topic entities")
    public ResponseEntity<Map<String, Object>> getTopics() {
        List<PageMetadata> pages = crawlerEngine.getAllCrawledPages();
        Map<String, Integer> catCounts = new HashMap<>();
        Map<String, Integer> keywordFrequencies = new HashMap<>();

        for (PageMetadata p : pages) {
            String cat = p.getCategory() != null ? p.getCategory() : "General";
            catCounts.merge(cat, 1, Integer::sum);
            if (p.getTopKeywords() != null) {
                for (String kw : p.getTopKeywords()) {
                    keywordFrequencies.merge(kw, 1, Integer::sum);
                }
            }
        }

        List<Map<String, Object>> topEntities = keywordFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> Map.<String, Object>of("keyword", e.getKey(), "documentCount", e.getValue()))
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("categories", catCounts);
        resp.put("topEntities", topEntities);
        resp.put("totalDiscoveredPages", pages.size());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Discovered Web Content", description = "Exports all discovered web page metadata and content as JSON or CSV")
    public ResponseEntity<?> exportContent(@RequestParam(defaultValue = "json") String format) {
        List<PageMetadata> pages = crawlerEngine.getAllCrawledPages();

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder();
            csv.append("URL,Title,StatusCode,Depth,WordCount,ReadingTimeMin,ContentHash,IsDuplicate,DuplicateOfUrl,OutboundLinksCount,TopKeywords\n");
            for (PageMetadata p : pages) {
                String cleanTitle = p.getTitle() != null ? p.getTitle().replace("\"", "\"\"") : "";
                String kw = p.getTopKeywords() != null ? String.join(";", p.getTopKeywords()) : "";
                csv.append(String.format("\"%s\",\"%s\",%d,%d,%d,%d,\"%s\",%b,\"%s\",%d,\"%s\"\n",
                        p.getUrl() != null ? p.getUrl() : "",
                        cleanTitle,
                        p.getStatusCode(),
                        p.getDepth(),
                        p.getWordCount(),
                        p.getReadingTimeMinutes(),
                        p.getContentHash() != null ? p.getContentHash() : "",
                        p.isDuplicateContent(),
                        p.getDuplicateOfUrl() != null ? p.getDuplicateOfUrl() : "",
                        p.getOutboundLinksCount(),
                        kw
                ));
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crawled_content.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.toString());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crawled_content.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pages);
    }

    @GetMapping("/status")
    @Operation(summary = "Get Crawl Status & Metrics", description = "Returns real-time crawling metrics including pages crawled, queue size, visited count, duplicates avoided, robots blocks, and throughput")
    public ResponseEntity<CrawlStats> getStatus() {
        return ResponseEntity.ok(crawlerEngine.getStats());
    }

    @GetMapping("/pages")
    @Operation(summary = "List Crawled Pages", description = "Returns metadata and discovered content for all pages crawled during the session")
    public ResponseEntity<List<PageMetadata>> getCrawledPages() {
        return ResponseEntity.ok(crawlerEngine.getAllCrawledPages());
    }

    @GetMapping("/page")
    @Operation(summary = "Get Single Page Metadata", description = "Retrieves cached page metadata, clean content, and discovered links for a specific URL")
    public ResponseEntity<PageMetadata> getPage(@RequestParam String url) {
        PageMetadata page = crawlerEngine.getPage(url);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(page);
    }

    @GetMapping("/visited")
    @Operation(summary = "Get Visited URLs Set", description = "Retrieves the complete set of distinct URLs stored in the Redis visited set")
    public ResponseEntity<Map<String, Object>> getVisitedUrls() {
        Set<String> visited = visitedUrlService.getAllVisited();
        Map<String, Object> result = new HashMap<>();
        result.put("totalVisited", visited.size());
        result.put("storage", visitedUrlService.isUsingRedis() ? "REDIS_SET" : "IN_MEMORY");
        result.put("urls", visited);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/queue")
    @Operation(summary = "Peek URL Frontier Queue", description = "Inspects pending URLs currently waiting in the frontier queue")
    public ResponseEntity<Map<String, Object>> getQueue(@RequestParam(defaultValue = "50") int limit) {
        List<CrawlTask> tasks = frontierQueue.peekAll(limit);
        Map<String, Object> result = new HashMap<>();
        result.put("queueSize", frontierQueue.size());
        result.put("storage", frontierQueue.isUsingRedis() ? "REDIS_LIST" : "IN_MEMORY");
        result.put("tasks", tasks);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/mock-urls")
    @Operation(summary = "List Sample Mock Dataset URLs", description = "Returns available internal mock web pages ready for instant crawling")
    public ResponseEntity<List<Map<String, String>>> getMockUrls() {
        List<Map<String, String>> urls = new ArrayList<>();
        urls.add(Map.of("name", "Root Portal Home", "url", "http://localhost:8080/mock-web/index.html", "type", "Multi-Domain Hub"));
        urls.add(Map.of("name", "Technology Hub", "url", "http://localhost:8080/mock-web/tech.html", "type", "Tech & AI Subgraph"));
        urls.add(Map.of("name", "Sports Portal", "url", "http://localhost:8080/mock-web/sports.html", "type", "Sports Subgraph"));
        urls.add(Map.of("name", "Duplicate Article A", "url", "http://localhost:8080/mock-web/duplicate-article-1.html", "type", "Near-Duplicate Detection"));
        urls.add(Map.of("name", "Cyclic Loop Node A", "url", "http://localhost:8080/mock-web/cyclic-a.html", "type", "Loop Prevention Test"));
        urls.add(Map.of("name", "External Boundary Test", "url", "http://localhost:8080/mock-web/external-links.html", "type", "Domain Boundary Filter"));
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/discovered")
    @Operation(summary = "Get All Discovered Websites", description = "Retrieves the complete catalog of all websites discovered across the crawl graph, with crawling state, parent provenance, and depth")
    public ResponseEntity<Map<String, Object>> getDiscoveredWebsites() {
        List<DiscoveredWebsite> list = crawlerEngine.getAllDiscoveredWebsites();
        long crawledCount = list.stream().filter(DiscoveredWebsite::isCrawled).count();
        long pendingCount = list.size() - crawledCount;

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalDiscovered", list.size());
        resp.put("crawledCount", crawledCount);
        resp.put("pendingCount", pendingCount);
        resp.put("websites", list);
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/page/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get Raw HTML of Crawled Website", description = "Retrieves the captured raw HTML source code of a crawled web page")
    public ResponseEntity<String> getPageHtml(@RequestParam String url) {
        PageMetadata page = crawlerEngine.getPage(url);
        if (page == null || page.getRawHtml() == null || page.getRawHtml().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(page.getRawHtml());
    }

    @GetMapping("/storage/info")
    @Operation(summary = "Get Crawled Content Disk Storage Status", description = "Returns metadata and statistics on the persistent disk archive where websites, raw HTML, and clean text are stored")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        return ResponseEntity.ok(crawlerEngine.getDataStorageService().getStorageInfo());
    }
}
