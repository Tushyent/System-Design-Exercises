package com.autocomplete.trie;

import com.autocomplete.dto.SuggestionDto;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TrieTest {

    @Test
    public void testTrieInsertAndSearch() {
        Trie trie = new Trie();
        trie.insert("apple", 100);
        trie.insert("application", 200);
        trie.insert("banana", 300);

        List<SuggestionDto> suggestions = trie.getTopK("app", 5);
        assertEquals(2, suggestions.size());
        
        // Sorting check (application has higher frequency 200 > 100, so it should be first)
        assertEquals("application", suggestions.get(0).getWord());
        assertEquals(200, suggestions.get(0).getFrequency());
        
        assertEquals("apple", suggestions.get(1).getWord());
        assertEquals(100, suggestions.get(1).getFrequency());
    }

    @Test
    public void testTrieIncrement() {
        Trie trie = new Trie();
        trie.insert("apple", 100);
        trie.incrementFrequency("apple", 50);

        List<SuggestionDto> suggestions = trie.getTopK("apple", 1);
        assertEquals(1, suggestions.size());
        assertEquals(150, suggestions.get(0).getFrequency());
    }

    @Test
    public void testTrieNoMatches() {
        Trie trie = new Trie();
        trie.insert("apple", 100);
        List<SuggestionDto> suggestions = trie.getTopK("xyz", 5);
        assertTrue(suggestions.isEmpty());
    }
}
