package com.library.config;

import com.library.autocomplete.PrefixTrie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrefixTrieConfig {

    @Bean
    public PrefixTrie prefixTrie() {
        return new PrefixTrie();
    }
}
