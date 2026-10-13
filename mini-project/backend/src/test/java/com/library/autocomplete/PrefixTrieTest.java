package com.library.autocomplete;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrefixTrieTest {

    @Test
    @DisplayName("Should retrieve suggestions by prefix case-insensitively with top score ordering")
    void testPrefixSearch() {
        PrefixTrie trie = new PrefixTrie();

        trie.insert(AutocompleteSuggestion.builder().id("1").text("Designing Data-Intensive Applications").type("TITLE").score(100).build());
        trie.insert(AutocompleteSuggestion.builder().id("2").text("Database System Concepts").type("TITLE").score(50).build());
        trie.insert(AutocompleteSuggestion.builder().id("3").text("Distributed Systems").type("CATEGORY").score(80).build());
        trie.insert(AutocompleteSuggestion.builder().id("4").text("Clean Code").type("TITLE").score(90).build());

        // Search "D" -> should return "Designing Data-Intensive Applications" (100), "Distributed Systems" (80), "Database System Concepts" (50)
        List<AutocompleteSuggestion> results = trie.search("d", 5);
        assertEquals(3, results.size());
        assertEquals("Designing Data-Intensive Applications", results.get(0).getText());
        assertEquals("Distributed Systems", results.get(1).getText());
        assertEquals("Database System Concepts", results.get(2).getText());

        // Search "clean"
        List<AutocompleteSuggestion> cleanResults = trie.search("CLEAN", 5);
        assertEquals(1, cleanResults.size());
        assertEquals("Clean Code", cleanResults.get(0).getText());

        // Search non-existent
        List<AutocompleteSuggestion> none = trie.search("xyz", 5);
        assertTrue(none.isEmpty());
    }
}
