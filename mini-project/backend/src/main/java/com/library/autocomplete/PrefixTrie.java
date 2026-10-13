package com.library.autocomplete;

import java.util.*;

public class PrefixTrie {

    private final TrieNode root = new TrieNode();

    public void insert(AutocompleteSuggestion suggestion) {
        if (suggestion == null || suggestion.getText() == null || suggestion.getText().isBlank()) {
            return;
        }

        String normalized = suggestion.getText().trim().toLowerCase(Locale.ROOT);
        TrieNode current = root;

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            current = current.getChildren().computeIfAbsent(ch, c -> new TrieNode());

            // Maintain top suggestions along the path
            addOrUpdateSuggestion(current.getSuggestions(), suggestion);
        }

        current.setEndOfWord(true);
    }

    private void addOrUpdateSuggestion(List<AutocompleteSuggestion> list, AutocompleteSuggestion newSug) {
        synchronized (list) {
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                AutocompleteSuggestion existing = list.get(i);
                if (existing.getId() != null && existing.getId().equals(newSug.getId())
                        && existing.getType().equals(newSug.getType())) {
                    list.set(i, newSug);
                    found = true;
                    break;
                }
            }
            if (!found) {
                list.add(newSug);
            }
            Collections.sort(list);
            // Bound to maximum 20 candidates per node to limit memory
            while (list.size() > 20) {
                list.remove(list.size() - 1);
            }
        }
    }

    public List<AutocompleteSuggestion> search(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = prefix.trim().toLowerCase(Locale.ROOT);
        TrieNode current = root;

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            current = current.getChildren().get(ch);
            if (current == null) {
                return Collections.emptyList();
            }
        }

        List<AutocompleteSuggestion> nodeSuggestions;
        synchronized (current.getSuggestions()) {
            nodeSuggestions = new ArrayList<>(current.getSuggestions());
        }

        Collections.sort(nodeSuggestions);
        if (nodeSuggestions.size() > limit) {
            return nodeSuggestions.subList(0, limit);
        }
        return nodeSuggestions;
    }
}
