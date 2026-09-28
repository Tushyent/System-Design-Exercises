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
@Schema(description = "Exact occurrence and line location of search term within a crawled document")
public class MatchOccurrenceDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Category of location: CONTENT_TITLE, CONTENT_BODY, METADATA_DESC, METADATA_KEYWORDS, HEADING")
    private String locationType;

    @Schema(description = "Human-readable label, e.g. 'Content-Title', 'Content (Line 3)', 'Metadata (Description)'")
    private String locationLabel;

    @Schema(description = "Standard section name: 'content-title', 'content', 'metadata'")
    private String section;

    @Schema(description = "Line number in body content if applicable, or -1")
    private int lineNumber;

    @Schema(description = "Highlighted snippet or text line with marked matches (<mark>keyword</mark>)")
    private String snippet;

    @Schema(description = "Clean plain text of snippet without HTML marks (useful for PDF export)")
    private String rawText;

    @Schema(description = "Number of keyword occurrences in this line/section")
    private int matchCount;
}
