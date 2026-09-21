package com.crawler;

import com.crawler.config.CrawlerProperties;
import com.crawler.model.DiscoveredWebsite;
import com.crawler.model.PageMetadata;
import com.crawler.service.CrawledDataStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataStorageServiceTest {

    private CrawledDataStorageService storageService;
    private CrawlerProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CrawlerProperties();
        storageService = new CrawledDataStorageService(properties);
        storageService.clear();
    }

    @AfterEach
    void tearDown() {
        storageService.clear();
    }

    @Test
    void testSavePagePersistsJsonHtmlAndText() {
        PageMetadata page = PageMetadata.builder()
                .url("http://localhost:8080/mock-web/test-page.html")
                .title("Test Storage Page")
                .statusCode(200)
                .cleanText("This is clean extracted body text for persistent storage verification.")
                .rawHtml("<!DOCTYPE html><html><head><title>Test</title></head><body><p>Test</p></body></html>")
                .wordCount(9)
                .readingTimeMinutes(1)
                .depth(0)
                .discoveredLinks(Arrays.asList("http://localhost:8080/mock-web/child1.html"))
                .outboundLinksCount(1)
                .build();

        storageService.savePage(page);

        Map<String, Object> info = storageService.getStorageInfo();
        assertNotNull(info);
        assertTrue((int) info.get("archivedFilesCount") >= 3, "Should archive .json, .html, and .txt files");

        Path root = storageService.getStorageRoot();
        File pagesDir = root.resolve("pages").toFile();
        assertTrue(pagesDir.exists() && pagesDir.isDirectory());

        File[] files = pagesDir.listFiles();
        assertNotNull(files);
        assertTrue(files.length >= 3);

        boolean hasJson = false;
        boolean hasHtml = false;
        boolean hasTxt = false;
        for (File f : files) {
            if (f.getName().endsWith(".json")) hasJson = true;
            if (f.getName().endsWith(".html")) hasHtml = true;
            if (f.getName().endsWith(".txt")) hasTxt = true;
        }
        assertTrue(hasJson, "JSON archive file must exist");
        assertTrue(hasHtml, "HTML archive file must exist");
        assertTrue(hasTxt, "Text archive file must exist");
    }

    @Test
    void testSaveDiscoveredWebsitesAndSummary() throws Exception {
        DiscoveredWebsite dw1 = DiscoveredWebsite.builder()
                .url("http://localhost:8080/mock-web/index.html")
                .parentUrl("SEED")
                .depth(0)
                .crawled(true)
                .statusCode(200)
                .title("Home")
                .build();

        DiscoveredWebsite dw2 = DiscoveredWebsite.builder()
                .url("http://localhost:8080/mock-web/tech.html")
                .parentUrl("http://localhost:8080/mock-web/index.html")
                .depth(1)
                .crawled(false)
                .build();

        storageService.recordDiscoveredUrl(dw1.getUrl());
        storageService.recordDiscoveredUrl(dw2.getUrl());
        storageService.saveDiscoveredWebsites(Arrays.asList(dw1, dw2));

        Path root = storageService.getStorageRoot();
        Path sitesJson = root.resolve("discovered_websites.json");
        Path urlsTxt = root.resolve("discovered_urls.txt");

        assertTrue(Files.exists(sitesJson), "discovered_websites.json should exist");
        assertTrue(Files.exists(urlsTxt), "discovered_urls.txt should exist");

        String urlsContent = Files.readString(urlsTxt);
        assertTrue(urlsContent.contains("http://localhost:8080/mock-web/index.html"));
        assertTrue(urlsContent.contains("http://localhost:8080/mock-web/tech.html"));

        PageMetadata pm = PageMetadata.builder()
                .url("http://localhost:8080/mock-web/index.html")
                .title("Home")
                .statusCode(200)
                .cleanText("Welcome home")
                .rawHtml("<html><body>Welcome home</body></html>")
                .build();

        storageService.saveSummary(Collections.singletonList(pm));
        Path summaryCsv = root.resolve("crawled_summary.csv");
        Path allJson = root.resolve("all_crawled_content.json");
        assertTrue(Files.exists(summaryCsv));
        assertTrue(Files.exists(allJson));
    }
}
