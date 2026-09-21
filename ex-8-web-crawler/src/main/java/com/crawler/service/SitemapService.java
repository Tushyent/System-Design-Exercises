package com.crawler.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Service to automatically discover and extract URLs from standard XML Sitemaps (sitemaps.org protocol).
 */
@Slf4j
@Service
public class SitemapService {

    private final UrlValidator urlValidator;

    public SitemapService(UrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    /**
     * Attempts to discover sitemap URLs for a given seed URL.
     * Checks common locations such as /sitemap.xml and any sitemaps declared in robots.txt.
     */
    public List<String> discoverAndParseSitemap(String seedUrl, List<String> declaredSitemaps, List<String> allowedDomains) {
        Set<String> discoveredUrls = new LinkedHashSet<>();
        Set<String> sitemapTargets = new LinkedHashSet<>();

        if (declaredSitemaps != null) {
            sitemapTargets.addAll(declaredSitemaps);
        }

        try {
            URI uri = URI.create(seedUrl);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String host = uri.getHost();
            int port = uri.getPort();
            String rootUrl = scheme + "://" + host + (port != -1 ? ":" + port : "");
            sitemapTargets.add(rootUrl + "/sitemap.xml");
            sitemapTargets.add(rootUrl + "/mock-web/sitemap.xml");
        } catch (Exception ignored) {}

        for (String sitemapUrl : sitemapTargets) {
            try {
                List<String> parsed = parseSitemapUrl(sitemapUrl, allowedDomains);
                if (!parsed.isEmpty()) {
                    discoveredUrls.addAll(parsed);
                    log.info("Discovered {} URLs from sitemap: {}", parsed.size(), sitemapUrl);
                }
            } catch (Exception e) {
                log.debug("No sitemap accessible at {}: {}", sitemapUrl, e.getMessage());
            }
        }

        return new ArrayList<>(discoveredUrls);
    }

    /**
     * Fetches and parses an XML sitemap document, extracting all valid `<loc>` URLs.
     */
    public List<String> parseSitemapUrl(String sitemapUrl, List<String> allowedDomains) {
        List<String> urls = new ArrayList<>();
        try {
            URL u = new URL(sitemapUrl);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "WebCrawler/1.0");

            if (conn.getResponseCode() != 200) {
                return urls;
            }

            try (InputStream in = conn.getInputStream()) {
                Document doc = Jsoup.parse(in, "UTF-8", sitemapUrl, Parser.xmlParser());

                // Check for Sitemap Index (<sitemapindex><sitemap><loc>...</loc></sitemap></sitemapindex>)
                Elements sitemapLocs = doc.select("sitemapindex > sitemap > loc");
                if (!sitemapLocs.isEmpty()) {
                    for (Element loc : sitemapLocs) {
                        String subSitemap = loc.text().trim();
                        urls.addAll(parseSitemapUrl(subSitemap, allowedDomains));
                    }
                    return urls;
                }

                // Check for standard URL Set (<urlset><url><loc>...</loc></url></urlset>)
                Elements urlLocs = doc.select("url > loc, urlset > url > loc");
                for (Element loc : urlLocs) {
                    String rawUrl = loc.text().trim();
                    String validated = urlValidator.normalizeAndValidate(rawUrl, allowedDomains);
                    if (validated != null) {
                        urls.add(validated);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error parsing sitemap {}: {}", sitemapUrl, e.getMessage());
        }
        return urls;
    }
}
