package com.example.urlshortener.service;


import com.example.urlshortener.model.UrlModel;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.util.HashUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UrlService {
    UrlRepository repo;
    public UrlService(UrlRepository repo){
        this.repo = repo;
    }

    public ResponseEntity<Map<String,String>> shortenUrl(String longUrl){
        Map<String, String> response = new HashMap<>();
        Optional<UrlModel> existing = repo.findBylongUrl(longUrl);

        if(existing.isPresent()){
            response.put("shortUrl","http://localhost:8080/" + existing.get().getShortCode());
        }
        else{
            String shortCode = HashUtil.generateShortCode(longUrl);
            while (repo.existsByShortCode(shortCode)) {
                shortCode = shortCode + "A";
            }

            UrlModel url = new UrlModel();
            url.setLongUrl(longUrl);
            url.setShortCode(shortCode);
            url.setCreatedAt(String.valueOf(LocalDateTime.now()));
            repo.save(url);
            response.put("shortUrl", "http://localhost:8080/" + shortCode);
        }
        return ResponseEntity.ok(response);
    }

    public String getOriginalUrl(String shortCode) {
        UrlModel url = repo.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        return url.getLongUrl();
    }

}
