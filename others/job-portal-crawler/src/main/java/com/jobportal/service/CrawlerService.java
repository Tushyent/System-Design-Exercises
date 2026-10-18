package com.jobportal.service;

import com.jobportal.config.CrawlerProperties;
import com.jobportal.dto.CrawlResponse;
import com.jobportal.dto.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Question (a) — job-listing crawler (BFS, max 10 pages per request):
 *
 * <ol>
 *   <li>Seed the FIFO {@link UrlQueueService} with the seed URL (when new).</li>
 *   <li>Repeat up to {@code maxPagesPerRequest} times: dequeue head URL, skip
 *       when already in the {@link VisitedUrlService} set, otherwise mark
 *       visited, fetch the page, and enqueue newly discovered job links.</li>
 *   <li>Return crawled pages, discovered URLs, visited URLs and queue remainder.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final CrawlerProperties props;
    private final UrlQueueService urlQueue;
    private final VisitedUrlService visited;
    private final PageFetcherService fetcher;
    private final UrlValidator validator;

    public CrawlResponse crawl(String rawSeedUrl, String clientId, double tokensRemaining) {
        String seedUrl = validator.normalizeAndValidate(rawSeedUrl);
        if (seedUrl == null) {
            throw new IllegalArgumentException("Invalid seed URL: " + rawSeedUrl);
        }

        int maxPages = props.getMaxPagesPerRequest();
        List<PageResult> pages = new ArrayList<>();
        Set<String> discoveredThisRequest = new LinkedHashSet<>();
        int duplicatesSkipped = 0;

        // Seed the queue when this URL graph has not been seen before.
        if (!visited.isVisited(seedUrl)) {
            urlQueue.enqueue(seedUrl);
        } else {
            duplicatesSkipped++;
            log.info("Seed {} already visited — continuing from queue (size={})", seedUrl, urlQueue.size());
        }

        int crawled = 0;
        while (crawled < maxPages) {
            String next = urlQueue.dequeue();
            if (next == null) {
                break; // queue drained
            }
            // Visited-set gate: never fetch the same listing twice.
            if (!visited.markVisitedIfAbsent(next)) {
                duplicatesSkipped++;
                continue;
            }
            PageFetcherService.FetchedPage page = fetcher.fetch(next);
            crawled++;

            List<String> newJobLinks = new ArrayList<>();
            // Prefer job-posting links; fall back to all links so the crawl
            // still expands on pages without job-looking anchors.
            List<String> candidates = page.getJobLinks().isEmpty() ? page.getAllLinks() : page.getJobLinks();
            for (String link : candidates) {
                if (!visited.isVisited(link)) {
                    urlQueue.enqueue(link);
                    newJobLinks.add(link);
                    discoveredThisRequest.add(link);
                } else {
                    duplicatesSkipped++;
                }
            }
            pages.add(new PageResult(next, page.getTitle(), page.getStatusCode(), newJobLinks, false));
            log.info("Crawled [{}/{}] {} -> {} new job links", crawled, maxPages, next, newJobLinks.size());
        }

        Set<String> visitedUrls = visited.getAllVisited();
        return new CrawlResponse(
                seedUrl,
                clientId,
                pages,
                new ArrayList<>(discoveredThisRequest),
                new ArrayList<>(visitedUrls),
                visitedUrls.size(),
                urlQueue.size(),
                crawled,
                maxPages,
                duplicatesSkipped,
                Math.floor(tokensRemaining * 100.0) / 100.0,
                "Crawled " + crawled + " page(s) (max " + maxPages + "). "
                        + discoveredThisRequest.size() + " new URL(s) discovered, "
                        + urlQueue.size() + " URL(s) remaining in queue, "
                        + visitedUrls.size() + " URL(s) visited in total.");
    }

    public void reset() {
        urlQueue.clear();
        visited.clear();
    }
}
