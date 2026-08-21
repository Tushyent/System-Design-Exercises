package com.autocomplete.trie;

import com.autocomplete.dto.SuggestionDto;
import java.util.ArrayList;
import java.util.List;

public class Trie {
    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    /**
     * Inserts a word with a specific search frequency.
     */
    public void insert(String word, int frequency) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        TrieNode current = root;
        String normalized = word.toLowerCase().trim();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            current = current.getChildren().computeIfAbsent(ch, k -> new TrieNode());
        }
        current.setWord(normalized);
        current.setEndOfWord(true);
        current.setFrequency(frequency);
    }

    /**
     * Increments the frequency of a search word by 1 (or creates it if absent).
     */
    public void incrementFrequency(String word, int amount) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        TrieNode current = root;
        String normalized = word.toLowerCase().trim();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            current = current.getChildren().computeIfAbsent(ch, k -> new TrieNode());
        }
        current.setWord(normalized);
        current.setEndOfWord(true);
        current.setFrequency(current.getFrequency() + amount);
    }

    /**
     * Retrieves the top-K suggestions matching the prefix, sorted by frequency descending.
     */
    public List<SuggestionDto> getTopK(String prefix, int k) {
        List<SuggestionDto> results = new ArrayList<>();
        if (prefix == null) {
            return results;
        }
        
        String normalized = prefix.toLowerCase().trim();
        TrieNode current = root;
        
        // Traverse to the end of the prefix
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            current = current.getChildren().get(ch);
            if (current == null) {
                return results; // Prefix not found
            }
        }
        
        // Gather all suggestions downstream of the prefix node
        List<SuggestionDto> allSuggestions = new ArrayList<>();
        dfsCollect(current, allSuggestions);
        
        // Sort: frequency descending, then alphabetically on tie
        allSuggestions.sort((a, b) -> {
            if (b.getFrequency() != a.getFrequency()) {
                return Integer.compare(b.getFrequency(), a.getFrequency());
            }
            return a.getWord().compareTo(b.getWord());
        });
        
        // Return sublist of top-K
        return allSuggestions.subList(0, Math.min(k, allSuggestions.size()));
    }

    private void dfsCollect(TrieNode node, List<SuggestionDto> list) {
        if (node == null) {
            return;
        }
        if (node.isEndOfWord()) {
            list.add(new SuggestionDto(node.getWord(), node.getFrequency()));
        }
        for (TrieNode child : node.getChildren().values()) {
            dfsCollect(child, list);
        }
    }
}
