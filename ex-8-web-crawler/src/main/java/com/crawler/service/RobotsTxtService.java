package com.crawler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for fetching, parsing, caching, and evaluating robots.txt compliance rules.
 */
@Slf4j
@Service
public class RobotsTxtService {

    public static class HostRules {
        private final List<String> disallowPaths = new ArrayList<>();
        private final List<String> allowPaths = new ArrayList<>();
        private final List<String> sitemaps = new ArrayList<>();
        private int crawlDelaySeconds = 0;

        public List<String> getDisallowPaths() { return disallowPaths; }
        public List<String> getAllowPaths() { return allowPaths; }
        public List<String> getSitemaps() { return sitemaps; }
        public int getCrawlDelaySeconds() { return crawlDelaySeconds; }
    }

    private final Map<String, HostRules> hostRulesCache = new ConcurrentHashMap<>();

    /**
     * Checks whether a target URL is allowed to be crawled according to robots.txt rules.
     */
    public boolean isUrlAllowed(String urlString, String userAgent) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(urlString);
            String host = uri.getHost();
            if (host == null) return true;

            int port = uri.getPort();
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String hostKey = scheme + "://" + host + (port != -1 ? ":" + port : "");

            HostRules rules = hostRulesCache.computeIfAbsent(hostKey, key -> fetchAndParse(key, userAgent));

            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            if (uri.getQuery() != null) {
                path = path + "?" + uri.getQuery();
            }

            // RFC 9309: Longest matching prefix rule takes precedence
            int longestAllow = -1;
            for (String allow : rules.getAllowPaths()) {
                if (matchesPath(path, allow) && allow.length() > longestAllow) {
                    longestAllow = allow.length();
                }
            }

            int longestDisallow = -1;
            for (String disallow : rules.getDisallowPaths()) {
                if (matchesPath(path, disallow) && disallow.length() > longestDisallow) {
                    longestDisallow = disallow.length();
                }
            }

            if (longestDisallow == -1) {
                return true; // no disallow rule matched
            }

            // If allow rule is strictly longer than disallow rule, allow wins; otherwise disallow wins
            boolean allowed = longestAllow > longestDisallow;
            if (!allowed) {
                log.info("Robots.txt disallowed path '{}' (disallow match len {} >= allow match len {})",
                        path, longestDisallow, longestAllow);
            }
            return allowed;
        } catch (Exception e) {
            log.debug("Error checking robots.txt for {}: {}", urlString, e.getMessage());
            return true; // Default allow on parser failure
        }
    }

    /**
     * Discovers sitemap URLs declared in robots.txt for the host of the given URL.
     */
    public List<String> getDeclaredSitemaps(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String host = uri.getHost();
            if (host == null) return Collections.emptyList();
            int port = uri.getPort();
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String hostKey = scheme + "://" + host + (port != -1 ? ":" + port : "");

            HostRules rules = hostRulesCache.computeIfAbsent(hostKey, key -> fetchAndParse(key, "*"));
            return new ArrayList<>(rules.getSitemaps());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Fetches and parses robots.txt for the given host.
     */
    public HostRules fetchAndParse(String hostRoot, String userAgent) {
        HostRules rules = new HostRules();
        String robotsUrl = hostRoot + "/robots.txt";

        try {
            URL u = new URL(robotsUrl);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", userAgent != null ? userAgent : "WebCrawler/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.debug("No robots.txt found at {} (HTTP {}), default allow all.", robotsUrl, status);
                return rules;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                boolean appliesToUs = true;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    int colonIdx = line.indexOf(':');
                    if (colonIdx == -1) continue;

                    String directive = line.substring(0, colonIdx).trim().toLowerCase();
                    String value = line.substring(colonIdx + 1).trim();

                    if ("user-agent".equals(directive)) {
                        appliesToUs = "*".equals(value) || (userAgent != null && value.toLowerCase().contains(userAgent.toLowerCase()));
                    } else if (appliesToUs) {
                        if ("disallow".equals(directive) && !value.isEmpty()) {
                            rules.getDisallowPaths().add(value);
                        } else if ("allow".equals(directive) && !value.isEmpty()) {
                            rules.getAllowPaths().add(value);
                        } else if ("crawl-delay".equals(directive)) {
                            try {
                                rules.crawlDelaySeconds = Integer.parseInt(value);
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    if ("sitemap".equals(directive) && !value.isEmpty()) {
                        rules.getSitemaps().add(value);
                    }
                }
            }
            log.info("Parsed robots.txt for {}: {} disallow, {} allow, {} sitemaps",
                    hostRoot, rules.getDisallowPaths().size(), rules.getAllowPaths().size(), rules.getSitemaps().size());
        } catch (Exception e) {
            log.debug("Unable to fetch {}: {}", robotsUrl, e.getMessage());
        }

        return rules;
    }

    private boolean matchesPath(String path, String rule) {
        if (rule == null || rule.isEmpty()) return false;
        if ("/".equals(rule)) return true;
        if (rule.endsWith("*")) {
            String prefix = rule.substring(0, rule.length() - 1);
            return path.startsWith(prefix);
        }
        return path.startsWith(rule);
    }

    public void clearCache() {
        hostRulesCache.clear();
    }
}
