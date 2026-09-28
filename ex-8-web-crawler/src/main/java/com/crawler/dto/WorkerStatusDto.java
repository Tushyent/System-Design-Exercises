package com.crawler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Real-time execution status of a crawler worker thread")
public class WorkerStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Worker identifier number (1, 2, 3...)")
    private int workerId;

    @Schema(description = "Thread name e.g. CrawlerWorker-1")
    private String threadName;

    @Schema(description = "Current lifecycle state: ACTIVE, IDLE, FETCHING, PARSING, DEDUPLICATING, ENQUEUING, COMPLETED")
    private String state;

    @Schema(description = "Target URL currently being processed by this thread")
    private String currentUrl;

    @Schema(description = "Exploration depth of current task")
    private int currentDepth;

    @Schema(description = "Number of pages successfully crawled by this worker thread")
    private int pagesProcessed;

    @Schema(description = "Description of last action executed by this worker")
    private String lastAction;

    @Schema(description = "Timestamp of last worker activity")
    private long lastActiveTime;
}
