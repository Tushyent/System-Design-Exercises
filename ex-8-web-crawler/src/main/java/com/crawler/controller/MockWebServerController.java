package com.crawler.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@Tag(name = "Mock Web Dataset", description = "Serves internal interconnected web resources, sitemaps, and robots.txt for reproducible crawler testing")
public class MockWebServerController {

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Root robots.txt", description = "Serves root robots.txt rules")
    public ResponseEntity<String> getRootRobotsTxt() {
        return getRobotsTxtContent();
    }

    @GetMapping(value = "/mock-web/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Mock web robots.txt", description = "Serves mock web robots.txt rules")
    public ResponseEntity<String> getMockRobotsTxt() {
        return getRobotsTxtContent();
    }

    private ResponseEntity<String> getRobotsTxtContent() {
        Resource resource = new ClassPathResource("mock-web/robots.txt");
        if (resource.exists()) {
            try {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                        .body(resource.getContentAsString(StandardCharsets.UTF_8));
            } catch (IOException ignored) {}
        }
        String defaultRobots = "User-agent: *\n" +
                "Disallow: /mock-web/private-admin.html\n" +
                "Disallow: /mock-web/secret/\n" +
                "Allow: /mock-web/\n" +
                "Sitemap: http://localhost:8080/mock-web/sitemap.xml\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .body(defaultRobots);
    }

    @GetMapping(value = {"/sitemap.xml", "/mock-web/sitemap.xml"}, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Mock XML Sitemap", description = "Serves XML sitemap for automated URL discovery")
    public ResponseEntity<String> getSitemapXml() {
        Resource resource = new ClassPathResource("mock-web/sitemap.xml");
        if (resource.exists()) {
            try {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8")
                        .body(resource.getContentAsString(StandardCharsets.UTF_8));
            } catch (IOException ignored) {}
        }
        String defaultSitemap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                "  <url><loc>http://localhost:8080/mock-web/index.html</loc></url>\n" +
                "  <url><loc>http://localhost:8080/mock-web/tech.html</loc></url>\n" +
                "  <url><loc>http://localhost:8080/mock-web/ai.html</loc></url>\n" +
                "  <url><loc>http://localhost:8080/mock-web/sports.html</loc></url>\n" +
                "  <url><loc>http://localhost:8080/mock-web/news.html</loc></url>\n" +
                "</urlset>\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8")
                .body(defaultSitemap);
    }

    @GetMapping(value = "/mock-web/{fileName}")
    @Operation(summary = "Serve a mock web page", description = "Returns sample HTML web pages with rich hyperlinks and metadata")
    public ResponseEntity<String> getMockPage(@PathVariable String fileName) {
        String resourcePath = "mock-web/" + fileName;
        Resource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body("<html><head><title>404 Not Found</title></head><body><h1>404 Page Not Found</h1><p>The requested mock resource does not exist.</p></body></html>");
        }

        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String mediaType = fileName.endsWith(".xml") ? MediaType.APPLICATION_XML_VALUE
                    : (fileName.endsWith(".txt") ? MediaType.TEXT_PLAIN_VALUE : MediaType.TEXT_HTML_VALUE);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaType + "; charset=UTF-8")
                    .body(content);
        } catch (IOException e) {
            log.error("Failed to read mock file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><h1>500 Internal Error</h1></body></html>");
        }
    }
}
