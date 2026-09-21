package com.crawler;

import com.crawler.model.CrawlTask;
import com.crawler.service.UrlFrontierQueue;
import com.crawler.service.VisitedUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrawlerServiceTest {

    private UrlFrontierQueue frontierQueue;
    private VisitedUrlService visitedUrlService;

    @BeforeEach
    void setUp() {
        frontierQueue = new UrlFrontierQueue(null);
        visitedUrlService = new VisitedUrlService(null);
    }

    @Test
    void testFrontierQueueOperations() {
        assertTrue(frontierQueue.isEmpty());

        frontierQueue.enqueue(CrawlTask.builder().url("http://localhost:8080/page1").depth(0).build());
        frontierQueue.enqueue(CrawlTask.builder().url("http://localhost:8080/page2").depth(1).build());

        assertEquals(2, frontierQueue.size());

        CrawlTask t1 = frontierQueue.dequeue();
        assertNotNull(t1);
        assertEquals("http://localhost:8080/page1", t1.getUrl());

        CrawlTask t2 = frontierQueue.dequeue();
        assertNotNull(t2);
        assertEquals("http://localhost:8080/page2", t2.getUrl());

        assertTrue(frontierQueue.isEmpty());
    }

    @Test
    void testVisitedSetDuplicatePrevention() {
        assertEquals(0, visitedUrlService.getVisitedCount());

        // First visit -> returns true (newly added)
        boolean firstVisit = visitedUrlService.markVisitedIfAbsent("http://localhost:8080/page1");
        assertTrue(firstVisit);
        assertEquals(1, visitedUrlService.getVisitedCount());
        assertTrue(visitedUrlService.isVisited("http://localhost:8080/page1"));

        // Second visit to identical URL -> returns false (duplicate avoided)
        boolean secondVisit = visitedUrlService.markVisitedIfAbsent("http://localhost:8080/page1");
        assertFalse(secondVisit);
        assertEquals(1, visitedUrlService.getVisitedCount());

        // New distinct URL
        boolean anotherVisit = visitedUrlService.markVisitedIfAbsent("http://localhost:8080/page2");
        assertTrue(anotherVisit);
        assertEquals(2, visitedUrlService.getVisitedCount());
    }
}
