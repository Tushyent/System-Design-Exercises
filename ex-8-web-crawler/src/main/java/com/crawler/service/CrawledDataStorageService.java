package com.crawler.service;

import com.crawler.config.CrawlerProperties;
import com.crawler.model.DiscoveredWebsite;
import com.crawler.model.PageMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service dedicated to capturing, persisting, and archiving all discovered websites
 * and their full contents (clean text, raw HTML, metadata, and link graphs) directly
 * to persistent storage on disk.
 */
@Slf4j
@Service
public class CrawledDataStorageService {

    private final ObjectMapper objectMapper;
    private final Path storageRoot;
    private final Path pagesDir;
    private final Set<String> recordedUrls = ConcurrentHashMap.newKeySet();

    public CrawledDataStorageService(CrawlerProperties properties) {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        // Default storage root is 'crawled_data' in current directory
        this.storageRoot = Paths.get("crawled_data").toAbsolutePath();
        this.pagesDir = storageRoot.resolve("pages");
        initStorageDirectories();
    }

    private void initStorageDirectories() {
        try {
            Files.createDirectories(pagesDir);
            log.info("Crawled data storage archive initialized at: {}", storageRoot);
        } catch (IOException e) {
            log.error("Failed to initialize crawled data directory: {}", e.getMessage());
        }
    }

    /**
     * Persists a crawled web page and its full contents (JSON metadata, raw HTML, and clean text) to disk.
     */
    public synchronized void savePage(PageMetadata page) {
        if (page == null || page.getUrl() == null) return;

        try {
            Files.createDirectories(pagesDir);
            String safeName = sanitizeFilename(page.getUrl());

            // 1. Save structured JSON metadata & contents
            Path jsonFile = pagesDir.resolve(safeName + ".json");
            objectMapper.writeValue(jsonFile.toFile(), page);

            // 2. Save raw HTML if available
            if (page.getRawHtml() != null && !page.getRawHtml().isEmpty()) {
                Path htmlFile = pagesDir.resolve(safeName + ".html");
                Files.writeString(htmlFile, page.getRawHtml(), StandardCharsets.UTF_8);
            }

            // 3. Save clean extracted text
            if (page.getCleanText() != null && !page.getCleanText().isEmpty()) {
                Path txtFile = pagesDir.resolve(safeName + ".txt");
                Files.writeString(txtFile, page.getCleanText(), StandardCharsets.UTF_8);
            }

            // Append URL to running discovered_urls.txt log if new
            if (recordedUrls.add(page.getUrl())) {
                appendDiscoveredUrl(page.getUrl());
            }

        } catch (Exception e) {
            log.warn("Error archiving crawled page {}: {}", page.getUrl(), e.getMessage());
        }
    }

    /**
     * Records a newly discovered website URL to the running discovered_urls.txt file.
     */
    public synchronized void recordDiscoveredUrl(String url) {
        if (url == null || !recordedUrls.add(url)) return;
        appendDiscoveredUrl(url);
    }

    private void appendDiscoveredUrl(String url) {
        try {
            Files.createDirectories(storageRoot);
            Path urlListFile = storageRoot.resolve("discovered_urls.txt");
            try (FileWriter fw = new FileWriter(urlListFile.toFile(), true)) {
                fw.write(url + System.lineSeparator());
            }
        } catch (IOException e) {
            log.warn("Failed to append URL to discovered_urls.txt: {}", e.getMessage());
        }
    }

    /**
     * Persists the complete registry of all discovered websites into discovered_websites.json.
     */
    public synchronized void saveDiscoveredWebsites(Collection<DiscoveredWebsite> websites) {
        if (websites == null) return;
        try {
            Files.createDirectories(storageRoot);
            Path filePath = storageRoot.resolve("discovered_websites.json");
            objectMapper.writeValue(filePath.toFile(), new ArrayList<>(websites));
            log.info("Saved {} discovered websites to {}", websites.size(), filePath);
        } catch (Exception e) {
            log.warn("Error saving discovered_websites.json: {}", e.getMessage());
        }
    }

    /**
     * Saves a complete export of all crawled pages and their contents as JSON and CSV summary.
     */
    public synchronized void saveSummary(List<PageMetadata> pages) {
        if (pages == null) return;
        try {
            Files.createDirectories(storageRoot);

            // 1. Master JSON containing all crawled pages and full contents
            Path allPagesJson = storageRoot.resolve("all_crawled_content.json");
            objectMapper.writeValue(allPagesJson.toFile(), pages);

            // 2. Summary CSV
            Path summaryCsv = storageRoot.resolve("crawled_summary.csv");
            try (FileWriter fw = new FileWriter(summaryCsv.toFile())) {
                fw.write("URL,Title,StatusCode,Depth,WordCount,ReadingTimeMin,OutboundLinksCount,Category,ContentHash\n");
                for (PageMetadata p : pages) {
                    String cleanTitle = p.getTitle() != null ? p.getTitle().replace("\"", "\"\"") : "";
                    fw.write(String.format("\"%s\",\"%s\",%d,%d,%d,%d,%d,\"%s\",\"%s\"\n",
                            p.getUrl() != null ? p.getUrl() : "",
                            cleanTitle,
                            p.getStatusCode(),
                            p.getDepth(),
                            p.getWordCount(),
                            p.getReadingTimeMinutes(),
                            p.getOutboundLinksCount(),
                            p.getCategory() != null ? p.getCategory() : "General",
                            p.getContentHash() != null ? p.getContentHash() : ""
                    ));
                }
            }

            log.info("Archived all {} crawled pages to {} and {}", pages.size(), allPagesJson, summaryCsv);
        } catch (Exception e) {
            log.warn("Error saving crawled summary files: {}", e.getMessage());
        }
    }

    /**
     * Clears all archived files and resets the storage folder.
     */
    public synchronized void clear() {
        recordedUrls.clear();
        try {
            if (Files.exists(pagesDir)) {
                try (var stream = Files.walk(pagesDir)) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }
            Path urlsFile = storageRoot.resolve("discovered_urls.txt");
            Files.deleteIfExists(urlsFile);
            Path sitesFile = storageRoot.resolve("discovered_websites.json");
            Files.deleteIfExists(sitesFile);
            Path allFile = storageRoot.resolve("all_crawled_content.json");
            Files.deleteIfExists(allFile);
            Path csvFile = storageRoot.resolve("crawled_summary.csv");
            Files.deleteIfExists(csvFile);

            initStorageDirectories();
            log.info("Crawled data storage archive reset.");
        } catch (Exception e) {
            log.warn("Error cleaning storage directory: {}", e.getMessage());
        }
    }

    /**
     * Returns disk archive diagnostics and status.
     */
    public Map<String, Object> getStorageInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("storagePath", storageRoot.toString());
        info.put("totalRecordedUrls", recordedUrls.size());

        int pageFileCount = 0;
        long totalBytes = 0;
        if (Files.exists(pagesDir)) {
            File[] files = pagesDir.toFile().listFiles();
            if (files != null) {
                pageFileCount = files.length;
                for (File f : files) {
                    totalBytes += f.length();
                }
            }
        }
        info.put("archivedFilesCount", pageFileCount);
        info.put("totalSizeBytes", totalBytes);
        info.put("totalSizeKb", Math.round(totalBytes / 1024.0 * 10.0) / 10.0);
        return info;
    }

    public Path getStorageRoot() {
        return storageRoot;
    }

    private String sanitizeFilename(String url) {
        if (url == null) return "unknown";
        String clean = url.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (clean.length() > 100) {
            clean = clean.substring(0, 80) + "_" + Integer.toHexString(url.hashCode());
        }
        return clean;
    }
}
