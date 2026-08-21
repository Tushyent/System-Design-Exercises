package com.autocomplete.trie;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class TrieNode {
    private final Map<Character, TrieNode> children;
    private boolean endOfWord;
    private int frequency;
    private String word;

    public TrieNode() {
        this.children = new ConcurrentHashMap<>();
        this.endOfWord = false;
        this.frequency = 0;
        this.word = null;
    }
}
