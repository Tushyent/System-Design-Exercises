package com.crawler.service;

import com.crawler.config.CrawlerProperties;
import com.crawler.dto.CrawlRequest;
import com.crawler.dto.SearchResponse;
import com.crawler.dto.SingleCrawlResponse;
import com.crawler.model.CrawlStats;
import com.crawler.model.CrawlTask;
import com.crawler.model.DiscoveredWebsite;
import com.crawler.model.PageMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class WebCrawlerEngine {

    private final CrawlerProperties properties;
    private final UrlValidator urlValidator;
    private final UrlFrontierQueue frontierQueue;
    private final VisitedUrlService visitedUrlService;
    private final PageCacheService pageCacheService;
    private final HtmlParserService htmlParserService;
    private final RobotsTxtService robotsTxtService;
    private final SitemapService sitemapService;
    private final ContentDeduplicationService contentDedupService;
    private final ContentSearchService contentSearchService;
    private final PageRankService pageRankService;
    private final CrawledDataStorageService dataStorageService;

    // Discovered Websites Registry (all URLs discovered throughout crawl graph)
    private final Map<String, DiscoveredWebsite> discoveredWebsites = new ConcurrentHashMap<>();

    // Concurrency and State Control
    private ExecutorService workerPool;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicInteger totalDiscovered = new AtomicInteger(0);
    private final AtomicInteger totalCrawled = new AtomicInteger(0);
    private final AtomicInteger duplicatesAvoided = new AtomicInteger(0);
    private final AtomicInteger duplicateContentAvoided = new AtomicInteger(0);
    private final AtomicInteger robotsDisallowedCount = new AtomicInteger(0);
    private final AtomicInteger sitemapsDiscovered = new AtomicInteger(0);
    private final AtomicInteger invalidUrlsFiltered = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    private String currentJobId = UUID.randomUUID().toString();
    private long startTime = 0;
    private long endTime = 0;
    private int configuredMaxDepth = 3;
    private int configuredMaxPages = 50;
    private List<String> configuredAllowedDomains = new ArrayList<>();
    private long configuredPolitenessDelayMs = 50;
    private boolean configuredRespectRobotsTxt = true;
    private boolean configuredDiscoverSitemaps = true;
    private boolean configuredDetectDuplicateContent = true;

    public WebCrawlerEngine(CrawlerProperties properties,
                            UrlValidator urlValidator,
                            UrlFrontierQueue frontierQueue,
                            VisitedUrlService visitedUrlService,
                            PageCacheService pageCacheService,
                            HtmlParserService htmlParserService,
                            RobotsTxtService robotsTxtService,
                            SitemapService sitemapService,
                            ContentDeduplicationService contentDedupService,
                            ContentSearchService contentSearchService,
                            PageRankService pageRankService,
                            CrawledDataStorageService dataStorageService) {
        this.properties = properties;
        this.urlValidator = urlValidator;
        this.frontierQueue = frontierQueue;
        this.visitedUrlService = visitedUrlService;
        this.pageCacheService = pageCacheService;
        this.htmlParserService = htmlParserService;
        this.robotsTxtService = robotsTxtService;
        this.sitemapService = sitemapService;
        this.contentDedupService = contentDedupService;
        this.contentSearchService = contentSearchService;
        this.pageRankService = pageRankService;
        this.dataStorageService = dataStorageService;
    }

    /**
     * Initiates an asynchronous crawl job using the configured seed URLs and limits.
     */
    public synchronized CrawlStats startCrawl(CrawlRequest request) {
        if (isRunning.get()) {
            log.warn("Crawl job {} is already running; stop it first before starting a new job.", currentJobId);
            return getStats();
        }

        // 1. Reset counters and clear state
        resetState();
        isRunning.set(true);
        currentJobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        startTime = System.currentTimeMillis();
        endTime = 0;

        // 2. Configure job limits and discovery flags
        this.configuredMaxDepth = (request != null && request.getMaxDepth() != null)
                ? request.getMaxDepth() : properties.getDefaultMaxDepth();
        this.configuredMaxPages = (request != null && request.getMaxPages() != null)
                ? request.getMaxPages() : properties.getDefaultMaxPages();
        this.configuredPolitenessDelayMs = (request != null && request.getPolitenessDelayMs() != null)
                ? request.getPolitenessDelayMs() : properties.getPolitenessDelayMs();
        this.configuredAllowedDomains = (request != null && request.getAllowedDomains() != null)
                ? new ArrayList<>(request.getAllowedDomains()) : new ArrayList<>();
        this.configuredRespectRobotsTxt = (request == null || request.getRespectRobotsTxt() == null)
                ? true : request.getRespectRobotsTxt();
        this.configuredDiscoverSitemaps = (request == null || request.getDiscoverSitemaps() == null)
                ? true : request.getDiscoverSitemaps();
        this.configuredDetectDuplicateContent = (request == null || request.getDetectDuplicateContent() == null)
                ? true : request.getDetectDuplicateContent();

        int workers = (request != null && request.getWorkers() != null)
                ? request.getWorkers() : properties.getDefaultWorkers();

        // 3. Process and enqueue seed URLs
        List<String> seeds = (request != null && request.getSeedUrls() != null && !request.getSeedUrls().isEmpty())
                ? request.getSeedUrls()
                : Collections.singletonList("http://localhost:8080/mock-web/index.html");

        for (String rawSeed : seeds) {
            String canonicalSeed = urlValidator.normalizeAndValidate(rawSeed, null);
            if (canonicalSeed != null) {
                if (configuredAllowedDomains.isEmpty()) {
                    try {
                        String host = java.net.URI.create(canonicalSeed).getHost();
                        if (host != null) configuredAllowedDomains.add(host);
                    } catch (Exception ignored) {}
                }
                frontierQueue.enqueue(CrawlTask.builder()
                        .url(canonicalSeed)
                        .depth(0)
                        .parentUrl("SEED")
                        .build());
                totalDiscovered.incrementAndGet();
                registerDiscoveredWebsite(canonicalSeed, "SEED", 0);
                log.info("Enqueued seed URL: {}", canonicalSeed);

                // Automated Sitemap Discovery
                if (configuredDiscoverSitemaps) {
                    try {
                        List<String> declaredSitemaps = robotsTxtService.getDeclaredSitemaps(canonicalSeed);
                        List<String> sitemapUrls = sitemapService.discoverAndParseSitemap(canonicalSeed, declaredSitemaps, configuredAllowedDomains);
                        for (String smUrl : sitemapUrls) {
                            registerDiscoveredWebsite(smUrl, "SITEMAP", 1);
                            if (!visitedUrlService.isVisited(smUrl)) {
                                frontierQueue.enqueue(CrawlTask.builder()
                                        .url(smUrl)
                                        .depth(1)
                                        .parentUrl("SITEMAP")
                                        .build());
                                totalDiscovered.incrementAndGet();
                                sitemapsDiscovered.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Sitemap discovery exception on seed {}: {}", canonicalSeed, e.getMessage());
                    }
                }
            } else {
                invalidUrlsFiltered.incrementAndGet();
                log.warn("Invalid seed URL ignored: {}", rawSeed);
            }
        }

        // 4. Initialize thread pool and launch workers
        workerPool = Executors.newFixedThreadPool(workers, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "CrawlerWorker-" + (++counter));
                t.setDaemon(true);
                return t;
            }
        });

        for (int i = 0; i < workers; i++) {
            workerPool.submit(this::workerLoop);
        }

        log.info("Started crawler job '{}' with {} workers. Max Depth: {}, Max Pages: {}, Robots: {}, Sitemaps: {}, ContentDedup: {}",
                currentJobId, workers, configuredMaxDepth, configuredMaxPages,
                configuredRespectRobotsTxt, configuredDiscoverSitemaps, configuredDetectDuplicateContent);

        return getStats();
    }

    /**
     * Background worker execution loop.
     */
    private void workerLoop() {
        while (isRunning.get()) {
            if (totalCrawled.get() >= configuredMaxPages) {
                log.info("Reached maximum target page crawl limit ({}); terminating worker.", configuredMaxPages);
                stopCrawl();
                break;
            }

            CrawlTask task = frontierQueue.dequeue();
            if (task == null) {
                if (activeWorkers.get() == 0 && frontierQueue.isEmpty()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {}

                    if (activeWorkers.get() == 0 && frontierQueue.isEmpty()) {
                        log.info("URL frontier queue is depleted and all workers are idle. Crawl complete.");
                        stopCrawl();
                        break;
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            activeWorkers.incrementAndGet();
            try {
                processTask(task);
            } catch (Exception e) {
                log.error("Unexpected worker exception on {}: {}", task.getUrl(), e.getMessage());
                errorCount.incrementAndGet();
            } finally {
                activeWorkers.decrementAndGet();
            }

            // Politeness delay
            if (configuredPolitenessDelayMs > 0) {
                try {
                    Thread.sleep(configuredPolitenessDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Registers a URL discovered during crawling into the discovered websites catalog.
     */
    public void registerDiscoveredWebsite(String url, String parentUrl, int depth) {
        if (url == null) return;
        discoveredWebsites.computeIfAbsent(url, u -> {
            dataStorageService.recordDiscoveredUrl(u);
            return DiscoveredWebsite.builder()
                    .url(u)
                    .parentUrl(parentUrl)
                    .depth(depth)
                    .discoveredAt(System.currentTimeMillis())
                    .crawled(false)
                    .build();
        });
    }

    /**
     * Executes the crawling logic for a single task dequeued from the frontier.
     */
    private void processTask(CrawlTask task) {
        String url = task.getUrl();

        // 1. Robots.txt Compliance Check
        if (configuredRespectRobotsTxt && !robotsTxtService.isUrlAllowed(url, properties.getUserAgent())) {
            robotsDisallowedCount.incrementAndGet();
            DiscoveredWebsite dwDisallow = discoveredWebsites.get(url);
            if (dwDisallow != null) {
                dwDisallow.setStatusCode(403);
            }
            log.info("Skipping URL disallowed by robots.txt: {}", url);
            return;
        }

        // 2. Atomic URL-level Duplicate Check using Redis Set
        boolean isNew = visitedUrlService.markVisitedIfAbsent(url);
        if (!isNew) {
            duplicatesAvoided.incrementAndGet();
            log.debug("Skipping already visited URL: {}", url);
            return;
        }

        // 3. Fetch and parse web page (extracts clean text, raw HTML, headings, and metadata)
        PageMetadata metadata = htmlParserService.fetchAndParse(
                url, task.getDepth(), task.getParentUrl(), configuredAllowedDomains);

        // Update discovered website registry entry with crawl results
        DiscoveredWebsite dw = discoveredWebsites.get(url);
        if (dw == null) {
            dw = DiscoveredWebsite.builder()
                    .url(url)
                    .parentUrl(task.getParentUrl())
                    .depth(task.getDepth())
                    .discoveredAt(System.currentTimeMillis())
                    .build();
            discoveredWebsites.put(url, dw);
        }
        dw.setCrawled(true);
        dw.setStatusCode(metadata.getStatusCode());
        dw.setTitle(metadata.getTitle());
        dw.setWordCount(metadata.getWordCount());
        dw.setContentHash(metadata.getContentHash());
        dw.setOutboundLinksCount(metadata.getOutboundLinksCount());

        // 4. Content Checksum & Near-Duplicate Detection
        String contentHash = contentDedupService.computeContentHash(metadata.getCleanText());
        metadata.setContentHash(contentHash);
        dw.setContentHash(contentHash);
        boolean isContentDuplicate = false;

        if (configuredDetectDuplicateContent && contentHash != null) {
            String originalUrl = contentDedupService.checkAndRegisterHash(contentHash, url);
            if (originalUrl != null && !originalUrl.equals(url)) {
                metadata.setDuplicateContent(true);
                metadata.setDuplicateOfUrl(originalUrl);
                duplicateContentAvoided.incrementAndGet();
                isContentDuplicate = true;
                log.info("Duplicate content detected at {} (matches original {})", url, originalUrl);
            }
        }

        // 5. Cache page metadata in Redis and Memory, and persist to disk archive
        pageCacheService.cachePage(metadata);
        dataStorageService.savePage(metadata);
        totalCrawled.incrementAndGet();

        // 6. Index into Full-Text Search Engine if not duplicate content
        if (!isContentDuplicate && metadata.getStatusCode() == 200) {
            contentSearchService.indexPage(metadata);
            dw.setCategory(metadata.getCategory());
        }

        if (metadata.getStatusCode() >= 400) {
            errorCount.incrementAndGet();
        }

        log.info("[Crawled #{}] Depth: {} | Status: {} | Words: {} | Links: {} | URL: {}",
                totalCrawled.get(), metadata.getDepth(), metadata.getStatusCode(),
                metadata.getWordCount(), metadata.getOutboundLinksCount(), url);

        // 7. Discover and enqueue new outbound links (skip if content is duplicate)
        if (!isContentDuplicate && task.getDepth() < configuredMaxDepth && totalCrawled.get() < configuredMaxPages) {
            for (String link : metadata.getDiscoveredLinks()) {
                registerDiscoveredWebsite(link, url, task.getDepth() + 1);
                if (visitedUrlService.isVisited(link)) {
                    duplicatesAvoided.incrementAndGet();
                    continue;
                }

                frontierQueue.enqueue(CrawlTask.builder()
                        .url(link)
                        .depth(task.getDepth() + 1)
                        .parentUrl(url)
                        .build());
                totalDiscovered.incrementAndGet();
            }
        }
    }

    /**
     * Stops the running crawler.
     */
    public synchronized CrawlStats stopCrawl() {
        if (isRunning.compareAndSet(true, false)) {
            endTime = System.currentTimeMillis();
            if (workerPool != null) {
                workerPool.shutdown();
            }
            // Automatically execute PageRank authority scoring across discovered link graph
            try {
                pageRankService.computePageRank(getAllCrawledPages());
                dataStorageService.saveDiscoveredWebsites(discoveredWebsites.values());
                dataStorageService.saveSummary(getAllCrawledPages());
            } catch (Exception e) {
                log.warn("PageRank / persistence computation error: {}", e.getMessage());
            }
            log.info("Crawler job '{}' stopped. Total crawled: {}, Discovered: {}, Duplicates avoided: {}",
                    currentJobId, totalCrawled.get(), totalDiscovered.get(), duplicatesAvoided.get());
        }
        return getStats();
    }

    /**
     * Resets crawler data, clears queues, visited sets, cached pages, search index, and dedup store.
     */
    public synchronized void resetState() {
        if (isRunning.get()) {
            stopCrawl();
        }
        frontierQueue.clear();
        visitedUrlService.clear();
        pageCacheService.clear();
        contentSearchService.clear();
        contentDedupService.clear();
        robotsTxtService.clearCache();
        pageRankService.clear();
        discoveredWebsites.clear();
        dataStorageService.clear();

        totalDiscovered.set(0);
        totalCrawled.set(0);
        duplicatesAvoided.set(0);
        duplicateContentAvoided.set(0);
        robotsDisallowedCount.set(0);
        sitemapsDiscovered.set(0);
        invalidUrlsFiltered.set(0);
        errorCount.set(0);
        startTime = 0;
        endTime = 0;
        log.info("Web crawler state, Redis storage, content search index, PageRank, and dedup cache reset.");
    }

    /**
     * Synchronously crawls a single URL. Checks Redis page cache first.
     */
    public SingleCrawlResponse crawlSingleUrl(String rawUrl) {
        long t0 = System.currentTimeMillis();
        String validUrl = urlValidator.normalizeAndValidate(rawUrl, null);
        if (validUrl == null) {
            return SingleCrawlResponse.builder()
                    .url(rawUrl)
                    .title("Invalid URL Format")
                    .statusCode(400)
                    .contentLength(0)
                    .crawlTimeMs(0)
                    .source("REJECTED")
                    .linksCount(0)
                    .links(Collections.emptyList())
                    .build();
        }

        // 1. Check Redis Cache
        PageMetadata cached = pageCacheService.getCachedPage(validUrl);
        if (cached != null) {
            long rtt = System.currentTimeMillis() - t0;
            return SingleCrawlResponse.builder()
                    .url(validUrl)
                    .title(cached.getTitle())
                    .statusCode(cached.getStatusCode())
                    .contentLength(cached.getContentLength())
                    .crawlTimeMs(rtt)
                    .source("REDIS_CACHE")
                    .linksCount(cached.getOutboundLinksCount())
                    .links(cached.getDiscoveredLinks())
                    .build();
        }

        // 2. Fetch live via Jsoup
        PageMetadata fresh = htmlParserService.fetchAndParse(validUrl, 0, "DIRECT_API", null);
        String contentHash = contentDedupService.computeContentHash(fresh.getCleanText());
        fresh.setContentHash(contentHash);

        pageCacheService.cachePage(fresh);
        visitedUrlService.markVisitedIfAbsent(validUrl);
        contentSearchService.indexPage(fresh);
        pageRankService.computePageRank(getAllCrawledPages());

        // Track and persist single crawl
        registerDiscoveredWebsite(validUrl, "DIRECT_API", 0);
        DiscoveredWebsite dw = discoveredWebsites.get(validUrl);
        if (dw != null) {
            dw.setCrawled(true);
            dw.setStatusCode(fresh.getStatusCode());
            dw.setTitle(fresh.getTitle());
            dw.setWordCount(fresh.getWordCount());
            dw.setContentHash(contentHash);
            dw.setOutboundLinksCount(fresh.getOutboundLinksCount());
        }
        dataStorageService.savePage(fresh);
        dataStorageService.saveDiscoveredWebsites(discoveredWebsites.values());

        long rtt = System.currentTimeMillis() - t0;
        return SingleCrawlResponse.builder()
                .url(validUrl)
                .title(fresh.getTitle())
                .statusCode(fresh.getStatusCode())
                .contentLength(fresh.getContentLength())
                .crawlTimeMs(rtt)
                .source("LIVE_FETCH")
                .linksCount(fresh.getOutboundLinksCount())
                .links(fresh.getDiscoveredLinks())
                .build();
    }

    /**
     * Executes full-text search across all discovered web content with ranking and filtering.
     */
    public SearchResponse searchContent(String query, String sortBy, String category) {
        return contentSearchService.search(query, sortBy, category);
    }

    public SearchResponse searchContent(String query) {
        return contentSearchService.search(query, "COMPOSITE", null);
    }

    public List<Map<String, Object>> getAutocomplete(String prefix, int limit) {
        return contentSearchService.getAutocompleteSuggestions(prefix, limit);
    }

    public Map<String, Double> computePageRank() {
        return pageRankService.computePageRank(getAllCrawledPages());
    }

    public Map<String, Double> getAllPageRanks() {
        return pageRankService.getAllPageRanks();
    }

    /**
     * Computes real-time metrics and crawler status.
     */
    public CrawlStats getStats() {
        long now = System.currentTimeMillis();
        long elapsed = startTime > 0 ? ((endTime > 0 ? endTime : now) - startTime) : 0;
        double throughput = elapsed > 0 ? (totalCrawled.get() * 1000.0 / elapsed) : 0.0;

        String status = isRunning.get() ? "RUNNING" : (totalCrawled.get() > 0 ? "COMPLETED" : "IDLE");
        String storage = (visitedUrlService.isUsingRedis() && frontierQueue.isUsingRedis()) ? "REDIS" : "IN_MEMORY";

        return CrawlStats.builder()
                .jobId(currentJobId)
                .status(status)
                .startTime(startTime)
                .endTime(endTime)
                .elapsedTimeMs(elapsed)
                .totalDiscovered(totalDiscovered.get())
                .totalCrawled(totalCrawled.get())
                .queueSize(frontierQueue.size())
                .visitedCount(visitedUrlService.getVisitedCount())
                .duplicateUrlsAvoided(duplicatesAvoided.get())
                .duplicateContentAvoided(duplicateContentAvoided.get())
                .robotsDisallowedCount(robotsDisallowedCount.get())
                .sitemapsDiscovered(sitemapsDiscovered.get())
                .invalidUrlsFiltered(invalidUrlsFiltered.get())
                .errorCount(errorCount.get())
                .throughputPagesPerSec(Math.round(throughput * 100.0) / 100.0)
                .storageMode(storage)
                .build();
    }

    public List<PageMetadata> getAllCrawledPages() {
        return pageCacheService.getAllPages();
    }

    public List<DiscoveredWebsite> getAllDiscoveredWebsites() {
        return new ArrayList<>(discoveredWebsites.values());
    }

    public CrawledDataStorageService getDataStorageService() {
        return dataStorageService;
    }

    public PageMetadata getPage(String url) {
        return pageCacheService.getCachedPage(url);
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
