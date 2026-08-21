package com.autocomplete.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutocompleteResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String query;
    private List<SuggestionDto> suggestions;
    private String source; // "CACHE" or "TRIE"
    private long executionTimeMs;
}
