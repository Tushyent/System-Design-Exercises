package com.crawler.service;

import com.crawler.config.CrawlerProperties;
import com.crawler.model.PageMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HtmlParserService {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing",
            "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
            "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
            "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
            "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours",
            "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should",
            "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them",
            "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've",
            "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd",
            "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's",
            "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't",
            "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
    ));

    private final CrawlerProperties properties;
    private final UrlValidator urlValidator;

    public HtmlParserService(CrawlerProperties properties, UrlValidator urlValidator) {
        this.properties = properties;
        this.urlValidator = urlValidator;
    }

    /**
     * Downloads a web page, parses HTML, extracts clean text, headings, metadata,
     * keywords, and collects all valid, canonicalized outbound hyperlinks.
     */
    public PageMetadata fetchAndParse(String targetUrl, int depth, String parentUrl, List<String> allowedDomains) {
        long startTime = System.currentTimeMillis();

        try {
            Connection connection = Jsoup.connect(targetUrl)
                    .userAgent(properties.getUserAgent())
                    .timeout(5000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true);

            Connection.Response response = connection.execute();
            int statusCode = response.statusCode();
            String contentType = response.contentType();
            long contentLength = response.bodyAsBytes().length;

            Document doc = response.parse();
            String rawHtml = doc.html();
            String title = doc.title();
            if (title == null || title.trim().isEmpty()) {
                title = "(No Title)";
            }

            // Extract Meta Tags
            String metaDesc = extractMeta(doc, "name", "description");
            if (metaDesc == null || metaDesc.isEmpty()) {
                metaDesc = extractMeta(doc, "property", "og:description");
            }
            String metaKeywords = extractMeta(doc, "name", "keywords");
            String author = extractMeta(doc, "name", "author");
            String ogImage = extractMeta(doc, "property", "og:image");
            
            // Canonical URL
            Element canonicalEl = doc.selectFirst("link[rel=canonical]");
            String canonicalUrl = canonicalEl != null ? canonicalEl.attr("href") : null;

            // Robots meta tag check
            String robotsMeta = extractMeta(doc, "name", "robots");
            boolean noFollow = robotsMeta != null && robotsMeta.toLowerCase().contains("nofollow");

            // Extract Headings (h1, h2, h3)
            List<String> headings = new ArrayList<>();
            Elements headingEls = doc.select("h1, h2, h3");
            for (Element h : headingEls) {
                String hText = h.text().trim();
                if (!hText.isEmpty() && headings.size() < 10) {
                    headings.add(hText);
                }
            }

            // Extract Clean Main Body Text (strip boilerplate nav, scripts, styles)
            Document cleanDoc = doc.clone();
            cleanDoc.select("nav, header, footer, script, style, aside, noscript, svg, form").remove();
            String cleanText = cleanDoc.body() != null ? cleanDoc.body().text().trim() : "";

            // Calculate Word Count and Reading Time
            int wordCount = 0;
            int readingTimeMinutes = 1;
            if (!cleanText.isEmpty()) {
                String[] words = cleanText.split("\\s+");
                wordCount = words.length;
                readingTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / 200.0));
            }

            // Extract Top Keywords (Frequency-based topic classification)
            List<String> topKeywords = extractTopKeywords(cleanText, title, metaKeywords);

            // Extract all <a href> hyperlinks (unless nofollow is set)
            Set<String> validLinks = new LinkedHashSet<>();
            if (!noFollow) {
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String rel = link.attr("rel");
                    if (rel != null && rel.toLowerCase().contains("nofollow")) {
                        continue;
                    }
                    String rawHref = link.attr("href");
                    String canonical = urlValidator.resolveAndValidate(targetUrl, rawHref, allowedDomains);
                    if (canonical != null) {
                        validLinks.add(canonical);
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;

            return PageMetadata.builder()
                    .url(targetUrl)
                    .title(title)
                    .statusCode(statusCode)
                    .contentType(contentType != null ? contentType : "text/html")
                    .contentLength(contentLength)
                    .crawlTimeMs(elapsed)
                    .depth(depth)
                    .parentUrl(parentUrl)
                    .discoveredLinks(new ArrayList<>(validLinks))
                    .outboundLinksCount(validLinks.size())
                    .cached(false)
                    .crawledAt(System.currentTimeMillis())
                    .cleanText(cleanText)
                    .rawHtml(rawHtml)
                    .metaDescription(metaDesc != null ? metaDesc : "")
                    .metaKeywords(metaKeywords != null ? metaKeywords : "")
                    .author(author != null ? author : "")
                    .openGraphImage(ogImage != null ? ogImage : "")
                    .canonicalUrl(canonicalUrl)
                    .headings(headings)
                    .wordCount(wordCount)
                    .readingTimeMinutes(readingTimeMinutes)
                    .topKeywords(topKeywords)
                    .build();

        } catch (HttpStatusException hse) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("HTTP {} returned when fetching {}: {}", hse.getStatusCode(), targetUrl, hse.getMessage());
            return PageMetadata.builder()
                    .url(targetUrl)
                    .title("HTTP Error " + hse.getStatusCode())
                    .statusCode(hse.getStatusCode())
                    .contentType("text/html")
                    .contentLength(0)
                    .crawlTimeMs(elapsed)
                    .depth(depth)
                    .parentUrl(parentUrl)
                    .discoveredLinks(new ArrayList<>())
                    .outboundLinksCount(0)
                    .cached(false)
                    .crawledAt(System.currentTimeMillis())
                    .cleanText("")
                    .rawHtml("")
                    .headings(Collections.emptyList())
                    .topKeywords(Collections.emptyList())
                    .build();

        } catch (IOException ioe) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("Network error fetching {}: {}", targetUrl, ioe.getMessage());
            return PageMetadata.builder()
                    .url(targetUrl)
                    .title("Network / Connection Failure")
                    .statusCode(503)
                    .contentType("none")
                    .contentLength(0)
                    .crawlTimeMs(elapsed)
                    .depth(depth)
                    .parentUrl(parentUrl)
                    .discoveredLinks(new ArrayList<>())
                    .outboundLinksCount(0)
                    .cached(false)
                    .crawledAt(System.currentTimeMillis())
                    .cleanText("")
                    .rawHtml("")
                    .headings(Collections.emptyList())
                    .topKeywords(Collections.emptyList())
                    .build();
        }
    }

    private String extractMeta(Document doc, String attrKey, String attrVal) {
        Element el = doc.selectFirst("meta[" + attrKey + "=" + attrVal + "]");
        return el != null ? el.attr("content").trim() : null;
    }

    private List<String> extractTopKeywords(String text, String title, String metaKeywords) {
        Map<String, Integer> freq = new HashMap<>();

        // Add meta keywords first with high weight
        if (metaKeywords != null && !metaKeywords.isEmpty()) {
            for (String kw : metaKeywords.split(",")) {
                String k = kw.trim().toLowerCase();
                if (k.length() >= 3 && !STOP_WORDS.contains(k)) {
                    freq.put(k, freq.getOrDefault(k, 0) + 5);
                }
            }
        }

        // Add title words
        if (title != null) {
            for (String w : title.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+")) {
                if (w.length() >= 3 && !STOP_WORDS.contains(w)) {
                    freq.put(w, freq.getOrDefault(w, 0) + 3);
                }
            }
        }

        // Add body words
        if (text != null && !text.isEmpty()) {
            for (String w : text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+")) {
                if (w.length() >= 3 && !STOP_WORDS.contains(w)) {
                    freq.put(w, freq.getOrDefault(w, 0) + 1);
                }
            }
        }

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(7)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
