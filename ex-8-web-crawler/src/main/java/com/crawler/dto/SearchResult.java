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
@Schema(description = "Discovered web content search hit")
public class SearchResult {

    @Schema(description = "URL of the matching page")
    private String url;

    @Schema(description = "Title of the page")
    private String title;

    @Schema(description = "Snippet excerpt highlighting matched terms")
    private String snippet;

    @Schema(description = "Meta description or excerpt")
    private String metaDescription;

    @Schema(description = "Calculated relevance score")
    private double relevanceScore;

    @Schema(description = "Word count of the page")
    private int wordCount;

    @Schema(description = "Estimated reading time in minutes")
    private int readingTimeMinutes;

    @Schema(description = "Exploration depth where page was discovered")
    private int depth;

    @Schema(description = "Keywords matching this page")
    private List<String> matchedTerms;

    @Schema(description = "Authority score computed via Google PageRank algorithm (0-100)")
    private double pageRankScore;

    @Schema(description = "Information retrieval textual score via Okapi BM25")
    private double bm25Score;

    @Schema(description = "Auto-classified topic category")
    private String category;

    @Schema(description = "Total occurrences of matched query terms in page")
    private int matchOccurrences;

    @Schema(description = "Detailed scoring breakdown (BM25 + PageRank + Field Weights)")
    private String scoreBreakdown;

    @Schema(description = "Discovered timestamp")
    private long crawledAt;
}
