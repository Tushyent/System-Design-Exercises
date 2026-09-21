package com.crawler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Synchronous response for single URL crawl request")
public class SingleCrawlResponse {

    @Schema(description = "Canonical requested URL", example = "http://localhost:8080/mock-web/index.html")
    private String url;

    @Schema(description = "Extracted HTML page title", example = "Mock Web Portal - Home")
    private String title;

    @Schema(description = "HTTP response status code", example = "200")
    private int statusCode;

    @Schema(description = "Content-Length in bytes", example = "854")
    private long contentLength;

    @Schema(description = "Elapsed processing time in milliseconds", example = "12")
    private long crawlTimeMs;

    @Schema(description = "Source of data: LIVE_FETCH or REDIS_CACHE", example = "REDIS_CACHE")
    private String source;

    @Schema(description = "Total valid outbound links discovered", example = "7")
    private int linksCount;

    @Schema(description = "List of extracted absolute hyperlinks")
    private List<String> links;
}
