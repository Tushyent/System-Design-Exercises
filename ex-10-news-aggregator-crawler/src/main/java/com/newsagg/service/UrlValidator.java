package com.newsagg.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates + canonicalises hyperlink targets. Only http/https news-article
 * style links survive; binary/media assets, mailto/javascript links, fragments
 * and tracking query params are stripped so the queue stays clean.
 */
@Slf4j
@Service
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("http", "https"));

    private static final Set<String> DISALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".svg", ".ico", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".tar", ".gz", ".7z", ".rar", ".exe", ".dmg",
            ".mp3", ".mp4", ".avi", ".mkv", ".mov", ".flv",
            ".css", ".js", ".woff", ".woff2", ".ttf", ".eot"));

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "ref", "fbclid", "gclid", "sessionid", "phpsessid", "jsessionid"));

    /**
     * Returns the canonical URL when valid, else null.
     */
    public String normalizeAndValidate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String url = rawUrl.trim();
        String lower = url.toLowerCase();
        if (lower.startsWith("mailto:") || lower.startsWith("javascript:")
                || lower.startsWith("tel:") || lower.startsWith("data:") || lower.startsWith("#")) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return null;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            host = host.toLowerCase();

            String path = uri.getPath() != null ? uri.getPath() : "";
            String lowerPath = path.toLowerCase();
            for (String ext : DISALLOWED_EXTENSIONS) {
                if (lowerPath.endsWith(ext)) {
                    return null;
                }
            }
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            int port = uri.getPort();
            if (("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443)) {
                port = -1;
            }
            URI canonical = new URI(
                    scheme.toLowerCase(), uri.getUserInfo(), host, port,
                    path.isEmpty() ? "/" : path,
                    normalizeQuery(uri.getQuery()), null);
            return canonical.normalize().toString();
        } catch (Exception e) {
            log.debug("URL rejected '{}': {}", url, e.getMessage());
            return null;
        }
    }

    /** Resolve a (possibly relative) href against a base page URL, then validate. */
    public String resolveAndValidate(String baseUrl, String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        try {
            URL resolved = new URL(new URL(baseUrl), href.trim());
            return normalizeAndValidate(resolved.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        java.util.List<String> keep = new java.util.ArrayList<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = (eq != -1 ? pair.substring(0, eq) : pair).toLowerCase();
            if (!TRACKING_PARAMS.contains(key)) {
                keep.add(pair);
            }
        }
        if (keep.isEmpty()) {
            return null;
        }
        java.util.Collections.sort(keep);
        return String.join("&", keep);
    }
}
