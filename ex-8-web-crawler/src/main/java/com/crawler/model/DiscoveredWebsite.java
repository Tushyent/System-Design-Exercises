package com.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a website / URL discovered during the crawling process,
 * capturing discovery metadata, parent provenance, crawl state, and content summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredWebsite implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Canonical URL of the discovered website */
    private String url;

    /** Provenance / referring source: "SEED", "SITEMAP", or parent page URL */
    private String parentUrl;

    /** Graph distance from initial seed (depth 0, 1, 2...) */
    private int depth;

    /** Epoch millisecond timestamp when URL was discovered */
    @Builder.Default
    private long discoveredAt = System.currentTimeMillis();

    /** Whether this website has been fetched and parsed */
    @Builder.Default
    private boolean crawled = false;

    /** HTTP response status code (e.g. 200, 404, 503), null if not crawled yet */
    private Integer statusCode;

    /** Extracted HTML document title */
    private String title;

    /** Classified topic category (e.g., Artificial Intelligence, Sports, Cloud) */
    private String category;

    /** Total body word count */
    private int wordCount;

    /** SHA-256 fingerprint of the normalized content */
    private String contentHash;

    /** Count of outbound hyperlinks discovered on this page */
    private int outboundLinksCount;
}
