package com.library.autocomplete;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrieNode {
    private final Map<Character, TrieNode> children = new ConcurrentHashMap<>();
    private final List<AutocompleteSuggestion> suggestions = Collections.synchronizedList(new ArrayList<>());
    private boolean isEndOfWord = false;

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public List<AutocompleteSuggestion> getSuggestions() {
        return suggestions;
    }

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }
}
