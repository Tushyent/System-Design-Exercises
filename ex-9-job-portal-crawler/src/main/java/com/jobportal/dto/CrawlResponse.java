package com.jobportal.dto;

import java.util.List;

/**
 * Response for POST /api/crawl — shows what one crawl request did.
 */
public record CrawlResponse(
        String seedUrl,
        String clientId,
        List<PageResult> crawledPages,
        List<String> discoveredUrls,
        List<String> visitedUrls,
        int visitedCount,
        int queueRemaining,
        int pagesCrawledThisRequest,
        int maxPagesPerRequest,
        int duplicatesSkipped,
        double tokensRemaining,
        String message) {
}
