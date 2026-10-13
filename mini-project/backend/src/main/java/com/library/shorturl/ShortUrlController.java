package com.library.shorturl;

import com.library.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @Data
    public static class ShortenRequest {
        private String targetUrl;
        private String resourceType;
        private String resourceId;
    }

    @PostMapping("/api/urls/shorten")
    public ResponseEntity<ApiResponse<ShortUrl>> shortenUrl(@RequestBody ShortenRequest request) {
        ShortUrl result = shortUrlService.shorten(request.getTargetUrl(), request.getResourceType(), request.getResourceId());
        return ResponseEntity.ok(ApiResponse.ok(result, "Short URL generated"));
    }

    @GetMapping("/s/{shortCode}")
    public ResponseEntity<Void> redirectShortUrl(
            @PathVariable String shortCode,
            HttpServletResponse response) throws IOException {

        String targetUrl = shortUrlService.resolve(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/urls/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUrlStats() {
        Map<String, Object> stats = Map.of(
                "totalResolutions", shortUrlService.getTotalResolutions(),
                "averageResolutionLatencyMs", shortUrlService.getAverageResolutionLatencyMs()
        );
        return ResponseEntity.ok(ApiResponse.ok(stats, "URL shortener statistics"));
    }
}
