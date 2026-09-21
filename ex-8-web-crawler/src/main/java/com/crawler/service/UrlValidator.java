package com.crawler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("http", "https"));

    private static final Set<String> DISALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".svg", ".ico", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".tar", ".gz", ".7z", ".rar", ".exe", ".dmg",
            ".mp3", ".mp4", ".avi", ".mkv", ".mov", ".flv",
            ".css", ".js", ".woff", ".woff2", ".ttf", ".eot"
    ));

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "ref", "fbclid", "gclid", "sessionid", "phpsessid", "jsessionid"
    ));

    /**
     * Validates and canonicalizes a URL string.
     * Returns the normalized URL string if valid, or null if invalid/rejected.
     */
    public String normalizeAndValidate(String rawUrl, List<String> allowedDomains) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }

        String url = rawUrl.trim();

        // 1. Filter non-web schemes immediately
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith("mailto:") || lowerUrl.startsWith("javascript:") 
                || lowerUrl.startsWith("tel:") || lowerUrl.startsWith("data:") 
                || lowerUrl.startsWith("#")) {
            return null;
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return null;
            }

            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return null;
            }
            host = host.toLowerCase();

            // 2. Check allowed domain boundary if specified
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                boolean domainAllowed = false;
                for (String domain : allowedDomains) {
                    if (domain != null && !domain.trim().isEmpty()) {
                        String cleanDomain = domain.trim().toLowerCase();
                        if (host.equals(cleanDomain) || host.endsWith("." + cleanDomain)) {
                            domainAllowed = true;
                            break;
                        }
                    }
                }
                if (!domainAllowed) {
                    return null;
                }
            }

            // 3. Filter binary and media file extensions
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String lowerPath = path.toLowerCase();
                for (String ext : DISALLOWED_EXTENSIONS) {
                    if (lowerPath.endsWith(ext)) {
                        return null;
                    }
                }
            } else {
                path = "";
            }

            // 4. Strip fragment (#anchor)
            // 5. Standardize default ports
            int port = uri.getPort();
            if (("http".equalsIgnoreCase(scheme) && port == 80) 
                    || ("https".equalsIgnoreCase(scheme) && port == 443)) {
                port = -1;
            }

            // 6. Strip trailing slash if path is longer than "/"
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // 7. Canonicalize Query String (filter tracking parameters and sort alphabetically)
            String cleanQuery = normalizeQuery(uri.getQuery());

            URI canonicalUri = new URI(
                    scheme.toLowerCase(),
                    uri.getUserInfo(),
                    host,
                    port,
                    path.isEmpty() ? "/" : path,
                    cleanQuery,
                    null // strip fragment
            );

            return canonicalUri.normalize().toString();

        } catch (Exception e) {
            log.debug("URL validation rejected '{}': {}", url, e.getMessage());
            return null;
        }
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return null;
        }
        String[] pairs = rawQuery.split("&");
        List<String> validPairs = new ArrayList<>();

        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            int eqIdx = pair.indexOf('=');
            String key = eqIdx != -1 ? pair.substring(0, eqIdx).toLowerCase() : pair.toLowerCase();
            if (!TRACKING_PARAMS.contains(key)) {
                validPairs.add(pair);
            }
        }

        if (validPairs.isEmpty()) {
            return null;
        }

        Collections.sort(validPairs);
        return String.join("&", validPairs);
    }

    /**
     * Resolves a potentially relative URL against a base URL and validates it.
     */
    public String resolveAndValidate(String baseUrl, String href, List<String> allowedDomains) {
        if (href == null || href.trim().isEmpty()) {
            return null;
        }
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, href.trim());
            return normalizeAndValidate(resolved.toString(), allowedDomains);
        } catch (Exception e) {
            return null;
        }
    }
}
