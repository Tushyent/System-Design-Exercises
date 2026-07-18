package com.example.urlshortener_red.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.urlshortener_red.dto.URLRequest;
import com.example.urlshortener_red.dto.URLResponse;
import com.example.urlshortener_red.service.URLService;

@RestController
public class URLController {

    @Autowired
    private URLService service;

    // POST /shorten
    @PostMapping("/shorten")
    public ResponseEntity<URLResponse> shortenURL(@RequestBody URLRequest request) {

        String shortCode = service.createShortURL(request.getLongUrl());

        URLResponse response = new URLResponse();

        response.setShortUrl("http://localhost:8080/" + shortCode);

        return ResponseEntity.ok(response);
    }

    // GET /{shortCode}
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode){
 
        Map<String,Object> data = service.getLongURL(shortCode);

        if(data == null){

            return ResponseEntity.notFound().build();

        }

        String longUrl = (String)data.get("longUrl");

        HttpHeaders headers = new HttpHeaders();

        headers.setLocation(URI.create(longUrl));

        return new ResponseEntity<>(headers,HttpStatus.FOUND);

    }

    @GetMapping("/details/{shortCode}")
    public ResponseEntity<Map<String,Object>> getDetails(
            @PathVariable String shortCode){

        Map<String,Object> response = service.getLongURL(shortCode);

        if(response == null){

            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        }

        return ResponseEntity.ok(response);

    }

}