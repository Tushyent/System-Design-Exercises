package com.library.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "crawler_jobs")
public class CrawlerJob {

    @Id
    private String id;
    private String jobId;
    private String sourceUrl;
    private String status; // "RUNNING", "COMPLETED", "FAILED"

    @Builder.Default
    private int recordsFetched = 0;

    @Builder.Default
    private int recordsParsed = 0;

    @Builder.Default
    private int recordsImported = 0;

    @Builder.Default
    private int recordsDeduplicated = 0;

    @Builder.Default
    private int recordsFailed = 0;

    private long durationMs;
    private double throughputBooksPerSec;
    private String errorMessage;

    @Builder.Default
    private Instant startedAt = Instant.now();

    private Instant finishedAt;
}
