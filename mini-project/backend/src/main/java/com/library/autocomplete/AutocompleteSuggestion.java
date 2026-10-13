package com.library.autocomplete;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutocompleteSuggestion implements Comparable<AutocompleteSuggestion> {
    private String id;
    private String text;
    private String type; // "TITLE", "AUTHOR", "CATEGORY"
    private int score;   // popularity or borrow frequency

    @Override
    public int compareTo(AutocompleteSuggestion o) {
        // Higher score comes first, then lexicographical
        int scoreCompare = Integer.compare(o.score, this.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        return this.text.compareToIgnoreCase(o.text);
    }
}
