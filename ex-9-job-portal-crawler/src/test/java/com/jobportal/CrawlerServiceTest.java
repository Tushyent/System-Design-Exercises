package com.jobportal;

import com.jobportal.config.CrawlerProperties;
import com.jobportal.ratelimit.InMemoryBucketStore;
import com.jobportal.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Crawler unit tests — run WITHOUT Redis (services fall back to in-memory
 * queue/set when Redis is unreachable). Uses a stub fetcher serving a fixed
 * link graph so link extraction/queue/visited logic is verified deterministically.
 */
class CrawlerServiceTest {

    /** Fixed in-memory "web": page -> outbound links. */
    static class StubFetcher extends PageFetcherService {
        private final java.util.Map<String, java.util.List<String>> web;

        StubFetcher(java.util.Map<String, java.util.List<String>> web) {
            super(new CrawlerProperties(), new UrlValidator());
            this.web = web;
        }

        @Override
        public FetchedPage fetch(String url) {
            java.util.List<String> links = web.getOrDefault(url, java.util.List.of());
            return new FetchedPage(url, "Title " + url, 200, links, links);
        }
    }

    private CrawlerService service(java.util.Map<String, java.util.List<String>> web) {
        CrawlerProperties props = new CrawlerProperties();
        props.setMaxPagesPerRequest(10);
        StringRedisTemplate broken = mock(StringRedisTemplate.class,
                inv -> { throw new RuntimeException("no redis in unit test"); });
        return new CrawlerService(props,
                new UrlQueueService(broken, props),
                new VisitedUrlService(broken, props),
                new StubFetcher(web),
                new UrlValidator());
    }

    @Test
    void crawlsMaxTenPagesAndReportsQueueRemainder() {
        // Chain of 15 pages: only 10 may be crawled per request.
        java.util.Map<String, java.util.List<String>> web = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 15; i++) {
            web.put("http://site/j" + i,
                    i < 14 ? java.util.List.of("http://site/j" + (i + 1)) : java.util.List.of());
        }
        var resp = service(web).crawl("http://site/j0", "u1", 4);
        assertEquals(10, resp.pagesCrawledThisRequest());
        assertEquals(10, resp.maxPagesPerRequest());
        assertEquals(1, resp.queueRemaining(), "j10..j14 chain: j10 crawled, j11 queued");
        assertEquals(10, resp.visitedCount());
        assertFalse(resp.discoveredUrls().isEmpty());
    }

    @Test
    void visitedSetPreventsDuplicateCrawling() {
        java.util.Map<String, java.util.List<String>> web = java.util.Map.of(
                "http://site/hub", java.util.List.of("http://site/a", "http://site/b"),
                "http://site/a", java.util.List.of("http://site/b", "http://site/hub"),
                "http://site/b", java.util.List.of("http://site/a"));
        var svc = service(web);
        var first = svc.crawl("http://site/hub", "u1", 4);
        assertEquals(3, first.visitedCount());
        var second = svc.crawl("http://site/hub", "u1", 3);
        assertEquals(0, second.pagesCrawledThisRequest(), "everything already visited");
        assertTrue(second.duplicatesSkipped() > 0);
        assertEquals(3, second.visitedCount(), "visited set never grows with duplicates");
    }

    @Test
    void invalidSeedIsRejected() {
        var svc = service(java.util.Map.of());
        assertThrows(IllegalArgumentException.class, () -> svc.crawl("not-a-url", "u1", 4));
    }

    @Test
    void tokenBucketAllowsFiveThenRejects() {
        var store = new InMemoryBucketStore();
        String client = "burst-client";
        int allowed = 0;
        for (int i = 0; i < 8; i++) {
            if (store.acquire(client, 5, 1.0, 1_000_000L).allowed()) {
                allowed++;
            }
        }
        assertEquals(5, allowed, "capacity 5 admits exactly 5 burst requests");
    }

    @Test
    void tokenBucketRefillsOnePerSecond() {
        var store = new InMemoryBucketStore();
        String client = "refill-client";
        long t0 = 2_000_000L;
        for (int i = 0; i < 5; i++) {
            assertTrue(store.acquire(client, 5, 1.0, t0).allowed());
        }
        assertFalse(store.acquire(client, 5, 1.0, t0).allowed(), "bucket drained");
        assertTrue(store.acquire(client, 5, 1.0, t0 + 1100).allowed(), "1 token refilled after ~1s");
    }

    @Test
    void urlValidatorFiltersNonJobAssets() {
        UrlValidator v = new UrlValidator();
        assertNull(v.normalizeAndValidate("mailto:hr@acme.com"));
        assertNull(v.normalizeAndValidate("http://site/logo.png"));
        assertNotNull(v.normalizeAndValidate("http://site/mock/jobs/job-101"));
        assertEquals("http://site/jobs?a=1&b=2",
                v.normalizeAndValidate("http://site/jobs/?b=2&utm_source=x&a=1#frag"));
    }
}
