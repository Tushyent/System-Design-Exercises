package com.example.urlshortener.controller;

import com.example.urlshortener.model.UrlModel;
import com.example.urlshortener.service.UrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/")
public class UrlController {
    UrlService service;
    UrlController(UrlService service){
        this.service = service;
    }
    @PostMapping("/shorten")
    public ResponseEntity<Map<String,String>> shortenUrl(@RequestBody UrlModel request){
        String longUrl = request.getLongUrl();
        return service.shortenUrl(longUrl);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode){
        String longUrl = service.getOriginalUrl(shortCode);
        return ResponseEntity.status(302).header("Location",longUrl).build();
    }
}
