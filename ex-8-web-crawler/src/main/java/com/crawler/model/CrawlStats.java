package com.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private String jobId;
    @Builder.Default
    private String status = "IDLE"; // IDLE, RUNNING, COMPLETED, STOPPED
    private long startTime;
    private long endTime;
    private long elapsedTimeMs;
    private int totalDiscovered;
    private int totalCrawled;
    private int queueSize;
    private int visitedCount;
    private int duplicateUrlsAvoided;
    private int duplicateContentAvoided;
    private int robotsDisallowedCount;
    private int sitemapsDiscovered;
    private int invalidUrlsFiltered;
    private int errorCount;
    private double throughputPagesPerSec;
    @Builder.Default
    private String storageMode = "REDIS"; // REDIS or IN_MEMORY
}
