package com.jobportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Input for POST /api/crawl. The seed URL is a job-listing page; the crawler
 * fetches it, extracts job hyperlinks, and BFS-expands up to 10 pages.
 */
public record CrawlRequest(
        @NotBlank(message = "seedUrl must not be blank")
        @Pattern(regexp = "^https?://.+", message = "seedUrl must start with http:// or https://")
        String seedUrl) {
}
