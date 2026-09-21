package com.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private String title;
    private int statusCode;
    private String contentType;
    private long contentLength;
    private long crawlTimeMs;
    private int depth;
    private String parentUrl;
    @Builder.Default
    private List<String> discoveredLinks = new ArrayList<>();
    private int outboundLinksCount;
    private boolean cached;
    @Builder.Default
    private long crawledAt = System.currentTimeMillis();

    // Automated Web Content Discovery Fields
    private String cleanText;
    private String rawHtml;
    private String metaDescription;
    private String metaKeywords;
    private String author;
    private String openGraphImage;
    private String canonicalUrl;
    @Builder.Default
    private List<String> headings = new ArrayList<>();
    private int wordCount;
    private int readingTimeMinutes;
    private String contentHash;
    private boolean duplicateContent;
    private String duplicateOfUrl;
    @Builder.Default
    private List<String> topKeywords = new ArrayList<>();
    private double pageRankScore;
    private String category;
}

