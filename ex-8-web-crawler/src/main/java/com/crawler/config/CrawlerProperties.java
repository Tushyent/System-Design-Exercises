package com.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    /** Default maximum crawling depth */
    private int defaultMaxDepth = 3;

    /** Default maximum pages to crawl in a batch */
    private int defaultMaxPages = 50;

    /** Default worker threads in pool */
    private int defaultWorkers = 4;

    /** Delay between consecutive page fetches in ms (politeness policy) */
    private long politenessDelayMs = 50;

    /** User-Agent header string sent with HTTP requests */
    private String userAgent = "SSN-WebCrawler/1.0 (+http://localhost:8080)";

    /** TTL in minutes for Redis-cached page metadata */
    private long cacheTtlMinutes = 60;
}
