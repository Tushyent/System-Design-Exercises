package com.autocomplete.service;

import com.autocomplete.dto.SuggestionDto;
import com.autocomplete.trie.Trie;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class TrieService {

    private final Trie trie;
    private final ResourceLoader resourceLoader;

    @Value("${autocomplete.dataset.path:classpath:dataset.csv}")
    private String datasetPath;

    public TrieService(ResourceLoader resourceLoader) {
        this.trie = new Trie();
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadDataset() {
        log.info("Loading autocomplete dataset from: {}", datasetPath);
        try {
            Resource resource = resourceLoader.getResource(datasetPath);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean isHeader = true;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String word = parts[0].trim();
                        try {
                            int freq = Integer.parseInt(parts[1].trim());
                            trie.insert(word, freq);
                            count++;
                        } catch (NumberFormatException e) {
                            log.warn("Skipping invalid frequency line: {}", line);
                        }
                    }
                }
                log.info("Successfully loaded {} terms into the Trie.", count);
            }
        } catch (Exception e) {
            log.error("Failed to load dataset: {}", e.getMessage(), e);
        }
    }

    public List<SuggestionDto> search(String prefix, int limit) {
        return trie.getTopK(prefix, limit);
    }

    public void incrementFrequency(String word, int amount) {
        trie.incrementFrequency(word, amount);
    }
}
