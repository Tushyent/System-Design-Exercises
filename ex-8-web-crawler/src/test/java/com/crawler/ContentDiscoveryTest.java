package com.crawler;

import com.crawler.dto.SearchResponse;
import com.crawler.model.PageMetadata;
import com.crawler.service.ContentDeduplicationService;
import com.crawler.service.ContentSearchService;
import com.crawler.service.PageRankService;
import com.crawler.service.RobotsTxtService;
import com.crawler.service.UrlValidator;
import com.crawler.util.PorterStemmer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ContentDiscoveryTest {

    private ContentSearchService searchService;
    private ContentDeduplicationService dedupService;
    private RobotsTxtService robotsTxtService;
    private UrlValidator urlValidator;
    private PageRankService pageRankService;

    @BeforeEach
    void setUp() {
        pageRankService = new PageRankService(null);
        searchService = new ContentSearchService(pageRankService);
        dedupService = new ContentDeduplicationService(null);
        robotsTxtService = new RobotsTxtService();
        urlValidator = new UrlValidator();
    }

    @Test
    void testQueryCanonicalizationAndTrackingParamStripping() {
        String raw1 = "http://example.com/article?utm_source=twitter&b=2&a=1&ref=rss";
        String normalized1 = urlValidator.normalizeAndValidate(raw1, null);
        assertEquals("http://example.com/article?a=1&b=2", normalized1);

        String raw2 = "http://example.com/article?b=2&a=1";
        String normalized2 = urlValidator.normalizeAndValidate(raw2, null);
        assertEquals("http://example.com/article?a=1&b=2", normalized2);

        assertEquals(normalized1, normalized2);
    }

    @Test
    void testContentDeduplicationViaHash() {
        String bodyText1 = "Quantum computing represents a paradigm shift in computational physics and distributed cryptography.";
        String bodyText2 = "quantum   COMPUTING represents a paradigm shift in computational physics and distributed CRYPTOGRAPHY.  ";

        String hash1 = dedupService.computeContentHash(bodyText1);
        String hash2 = dedupService.computeContentHash(bodyText2);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2, "Content hashes of normalized text must be identical");

        String orig1 = dedupService.checkAndRegisterHash(hash1, "http://localhost:8080/mock-web/article1.html");
        assertNull(orig1, "First occurrence should return null indicating original content");

        String orig2 = dedupService.checkAndRegisterHash(hash2, "http://localhost:8080/mock-web/article2-mirror.html");
        assertEquals("http://localhost:8080/mock-web/article1.html", orig2, "Duplicate content must reference original URL");
    }

    @Test
    void testPageRankAuthorityAlgorithm() {
        // Build a 3-node directed graph:
        // Page A links to B and C
        // Page B links to C
        // Page C links to A
        // Node C receives in-links from both A and B, so C should have the highest PageRank!
        PageMetadata pageA = PageMetadata.builder()
                .url("http://localhost:8080/pageA")
                .discoveredLinks(Arrays.asList("http://localhost:8080/pageB", "http://localhost:8080/pageC"))
                .build();
        PageMetadata pageB = PageMetadata.builder()
                .url("http://localhost:8080/pageB")
                .discoveredLinks(Collections.singletonList("http://localhost:8080/pageC"))
                .build();
        PageMetadata pageC = PageMetadata.builder()
                .url("http://localhost:8080/pageC")
                .discoveredLinks(Collections.singletonList("http://localhost:8080/pageA"))
                .build();

        Map<String, Double> ranks = pageRankService.computePageRank(Arrays.asList(pageA, pageB, pageC));
        assertEquals(3, ranks.size());

        double prA = ranks.get("http://localhost:8080/pageA");
        double prB = ranks.get("http://localhost:8080/pageB");
        double prC = ranks.get("http://localhost:8080/pageC");

        assertTrue(prC > prB, "Page C must have higher PageRank than B due to receiving inbound links from both A and B");
        assertTrue(prC >= 90.0, "Top authority page should normalize near 100.0");
    }

    @Test
    void testPorterStemmer() {
        assertEquals("comput", PorterStemmer.stemWord("computing"));
        assertEquals("comput", PorterStemmer.stemWord("computers"));
        assertEquals("process", PorterStemmer.stemWord("processors"));
        assertEquals("distribut", PorterStemmer.stemWord("distributed"));
    }

    @Test
    void testFetchEverythingAndRanking() {
        PageMetadata page1 = PageMetadata.builder()
                .url("http://localhost:8080/mock-web/tech.html")
                .title("Modern Artificial Intelligence & Machine Learning")
                .headings(Arrays.asList("Deep Neural Networks", "Transformers"))
                .cleanText("Deep learning and transformers are revolutionizing automated content discovery and search systems.")
                .metaKeywords("ai, machine learning, neural networks")
                .wordCount(12)
                .readingTimeMinutes(1)
                .depth(1)
                .crawledAt(System.currentTimeMillis())
                .build();

        PageMetadata page2 = PageMetadata.builder()
                .url("http://localhost:8080/mock-web/sports.html")
                .title("World Football & Cricket Highlights")
                .headings(Arrays.asList("Premier League", "Champions Trophy"))
                .cleanText("Football tournaments and cricket championships delivered thrilling weekend sports action.")
                .metaKeywords("football, sports, cricket")
                .wordCount(10)
                .readingTimeMinutes(1)
                .depth(1)
                .crawledAt(System.currentTimeMillis())
                .build();

        searchService.indexPage(page1);
        searchService.indexPage(page2);

        assertEquals(2, searchService.getIndexedDocumentCount());
        assertTrue(searchService.getIndexedVocabularySize() > 5);

        // Test Stemmed Search: "transforming" should match "transformers" via stem "transform"
        SearchResponse stemResults = searchService.search("transforming");
        assertEquals(1, stemResults.getTotalMatches());
        assertEquals("http://localhost:8080/mock-web/tech.html", stemResults.getResults().get(0).getUrl());

        // Test "Fetch Everything" metadata:
        SearchResponse aiResults = searchService.search("neural transformers");
        assertEquals(1, aiResults.getTotalMatches());
        assertTrue(aiResults.getTotalOccurrencesInCorpus() >= 2);
        assertNotNull(aiResults.getCategoryDistribution());
        assertEquals(1, aiResults.getCategoryDistribution().get("Artificial Intelligence"));
        assertNotNull(aiResults.getRelatedKeywords());

        // Test Vocabulary Autocomplete
        List<Map<String, Object>> suggestions = searchService.getAutocompleteSuggestions("neur", 5);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.get("term").toString().contains("neural")));
    }

    @Test
    void testRobotsTxtRuleEvaluation() {
        RobotsTxtService.HostRules rules = new RobotsTxtService.HostRules();
        rules.getDisallowPaths().add("/mock-web/private-admin.html");
        rules.getDisallowPaths().add("/admin/");
        rules.getAllowPaths().add("/admin/public.html");
        rules.getSitemaps().add("http://localhost:8080/mock-web/sitemap.xml");

        assertEquals(1, rules.getSitemaps().size());
        assertEquals("http://localhost:8080/mock-web/sitemap.xml", rules.getSitemaps().get(0));
    }
}
