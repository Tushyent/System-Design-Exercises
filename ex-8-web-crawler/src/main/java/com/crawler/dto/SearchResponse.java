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
@Schema(description = "Discovered web content search response")
public class SearchResponse {

    @Schema(description = "Search query term")
    private String query;

    @Schema(description = "Total matching documents discovered")
    private int totalMatches;

    @Schema(description = "Search execution time in milliseconds")
    private long searchTimeMs;

    @Schema(description = "Ranking strategy used (COMPOSITE, PAGERANK, BM25)")
    @Builder.Default
    private String rankingMode = "COMPOSITE";

    @Schema(description = "Total occurrences of searched keyword(s) across entire crawled corpus")
    private int totalOccurrencesInCorpus;

    @Schema(description = "Category distribution of matching pages")
    @Builder.Default
    private java.util.Map<String, Integer> categoryDistribution = new java.util.HashMap<>();

    @Schema(description = "Related topic keywords co-occurring with query")
    @Builder.Default
    private List<String> relatedKeywords = new java.util.ArrayList<>();

    @Schema(description = "Ranked list of search results")
    private List<SearchResult> results;
}
