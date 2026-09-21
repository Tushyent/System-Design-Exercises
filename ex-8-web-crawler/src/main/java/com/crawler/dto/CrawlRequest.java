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
@Schema(description = "Request body to configure and initiate a crawl session")
public class CrawlRequest {

    @Schema(description = "List of initial seed URLs", example = "[\"http://localhost:8080/mock-web/index.html\"]")
    private List<String> seedUrls;

    @Schema(description = "Maximum exploration depth from seed", example = "3")
    private Integer maxDepth;

    @Schema(description = "Maximum number of total pages to crawl", example = "50")
    private Integer maxPages;

    @Schema(description = "Number of concurrent worker threads", example = "4")
    private Integer workers;

    @Schema(description = "Allowed domain names (empty = allow all / stay on seed domain)", example = "[\"localhost\", \"127.0.0.1\"]")
    private List<String> allowedDomains;

    @Schema(description = "Delay between page downloads per worker in ms (politeness)", example = "50")
    private Long politenessDelayMs;

    @Schema(description = "Whether to parse and respect robots.txt exclusion rules", example = "true")
    @Builder.Default
    private Boolean respectRobotsTxt = true;

    @Schema(description = "Whether to automatically discover and parse sitemap.xml", example = "true")
    @Builder.Default
    private Boolean discoverSitemaps = true;

    @Schema(description = "Whether to detect and prevent indexing duplicate content using checksum hashing", example = "true")
    @Builder.Default
    private Boolean detectDuplicateContent = true;
}
