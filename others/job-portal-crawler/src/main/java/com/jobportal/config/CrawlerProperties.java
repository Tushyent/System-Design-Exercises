package com.jobportal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Crawler tuning parameters. Question (a) requires max 10 URLs per request.
 */
@Data
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    /** Maximum pages fetched in a single /api/crawl request. */
    private int maxPagesPerRequest = 10;

    /** HTTP fetch timeout in milliseconds. */
    private int fetchTimeoutMs = 6000;

    /** User-Agent sent to external job websites. */
    private String userAgent = "JobPortalCrawler/1.0 (+http://localhost:8080)";

    /** Redis key for the URL queue (FIFO list). */
    private String queueKey = "jobcrawl:queue";

    /** Redis key for the visited-URL set. */
    private String visitedKey = "jobcrawl:visited";
}
