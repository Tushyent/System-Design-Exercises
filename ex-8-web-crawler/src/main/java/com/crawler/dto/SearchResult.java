package com.crawler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Discovered web content search hit")
public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Rank position in search results (1-based)")
    private int rank;

    @Schema(description = "URL of the matching page")
    private String url;

    @Schema(description = "Title of the page")
    private String title;

    @Schema(description = "Highlighted title with matches marked")
    private String titleSnippet;

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

    // Location Specification Fields (content-title, content lines, metadata)
    @Schema(description = "Whether the term matched in content-title")
    private boolean matchedInTitle;

    @Schema(description = "Whether the term matched in page body content")
    private boolean matchedInContent;

    @Schema(description = "Whether the term matched in metadata (meta description, keywords, headings)")
    private boolean matchedInMetadata;

    @Schema(description = "Summary list of matching location tags e.g. ['Content-Title', 'Content (Line 3)', 'Metadata (Description)']")
    @Builder.Default
    private List<String> locationTags = new ArrayList<>();

    @Schema(description = "Detailed list of all exact occurrences, line numbers, and context snippets")
    @Builder.Default
    private List<MatchOccurrenceDetail> occurrences = new ArrayList<>();

    @Schema(description = "Human-readable summary of matched lines/sections")
    private String matchedLinesSummary;
}
