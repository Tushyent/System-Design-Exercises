package com.newsagg.service;

import com.newsagg.config.CrawlerProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fetches a webpage with Jsoup and extracts valid hyperlinks.
 * A link counts as a "article link" when its URL or anchor text looks like a
 * news article (story id, /article/, /news/..., section, headline).
 */
@Slf4j
@Service
public class PageFetcherService {

    private final CrawlerProperties props;
    private final UrlValidator validator;

    public PageFetcherService(CrawlerProperties props, UrlValidator validator) {
        this.props = props;
        this.validator = validator;
    }

    @Getter
    public static class FetchedPage {
        private final String url;
        private final String title;
        private final int statusCode;
        private final List<String> allLinks;
        private final List<String> articleLinks;

        public FetchedPage(String url, String title, int statusCode,
                           List<String> allLinks, List<String> articleLinks) {
            this.url = url;
            this.title = title;
            this.statusCode = statusCode;
            this.allLinks = allLinks;
            this.articleLinks = articleLinks;
        }
    }

    public FetchedPage fetch(String targetUrl) {
        try {
            Connection connection = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36 (NewsAggregatorCrawler/1.0)")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(Math.max(props.getFetchTimeoutMs(), 15000))
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .maxBodySize(2 * 1024 * 1024);
            Connection.Response response = connection.execute();
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                log.warn("Fetch {} returned HTTP {}", targetUrl, status);
                return new FetchedPage(targetUrl, "HTTP Error " + status, status, List.of(), List.of());
            }
            Document doc = response.parse();
            String title = doc.title() != null ? doc.title().trim() : targetUrl;

            Set<String> allLinks = new LinkedHashSet<>();
            Set<String> articleLinks = new LinkedHashSet<>();
            Elements anchors = doc.select("a[href]");
            for (Element a : anchors) {
                String href = a.attr("href");
                String text = a.text() != null ? a.text().trim() : "";
                String normalised = validator.resolveAndValidate(targetUrl, href);
                if (normalised == null) {
                    continue;
                }
                allLinks.add(normalised);
                if (isArticleLink(normalised, text)) {
                    articleLinks.add(normalised);
                }
            }
            // Fallback for JS-rendered front pages (headline JSON embedded, few anchors):
            // harvest article-ish URLs embedded anywhere in the raw HTML.
            if (articleLinks.isEmpty()) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(https?://[^\\s\"'<>]+|/[^\\s\"'<>]*?(?:story|stories|article|articles|news|section|world|politics|business|sport|cricket|election)[^\\s\"'<>]*)",
                                java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(doc.html());
                int guard = 0;
                while (m.find() && guard++ < 300) {
                    String cand = validator.resolveAndValidate(targetUrl, m.group(1));
                    if (cand != null && isArticleLink(cand, "")) {
                        allLinks.add(cand);
                        articleLinks.add(cand);
                    }
                }
            }
            return new FetchedPage(targetUrl, title, status,
                    new ArrayList<>(allLinks), new ArrayList<>(articleLinks));
        } catch (Exception e) {
            log.warn("Fetch failed for {}: {}", targetUrl, e.getMessage());
            return new FetchedPage(targetUrl, "Fetch Failed: " + e.getMessage(), 503, List.of(), List.of());
        }
    }

    /**
     * Heuristic: news-article hyperlinks usually contain story-ish path tokens
     * (/story, /article, /news/..., /2026/..., section names) or anchor text
     * that reads like a headline (verbs, topics, "read full story").
     */
    public boolean isArticleLink(String url, String anchorText) {
        String u = url.toLowerCase();
        String t = anchorText != null ? anchorText.toLowerCase() : "";
        if (u.contains("/story") || u.contains("story-") || u.contains("story_")
                || u.contains("/article") || u.contains("article-") || u.contains("article_")
                || u.contains("/news/") || u.contains("/section/") || u.contains("/world")
                || u.contains("/politics") || u.contains("/business") || u.contains("/sport")
                || u.contains("/tech") || u.contains("/science") || u.contains("/entertainment")
                || u.matches(".*/(19|20)\\d{2}/.*") || u.contains("?p=") || u.contains("?p-")) {
            return true;
        }
        if (t.length() > 25) {
            return true; // long anchors are almost always headlines
        }
        return t.contains("read full") || t.contains("read more") || t.contains("story")
                || t.contains("breaking") || t.contains("election") || t.contains("cricket")
                || t.contains("match") || t.contains("minister") || t.contains("market")
                || t.contains("launch") || t.contains("wins") || t.contains("record")
                || t.contains("announces") || t.contains("report") || t.contains("update");
    }
}
