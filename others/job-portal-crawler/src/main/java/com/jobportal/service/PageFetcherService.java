package com.jobportal.service;

import com.jobportal.config.CrawlerProperties;
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
 * A link counts as a "job link" when its URL or anchor text looks like a
 * job posting (job id, /jobs/, /careers/, apply, opening, ...).
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
        private final List<String> jobLinks;

        public FetchedPage(String url, String title, int statusCode,
                           List<String> allLinks, List<String> jobLinks) {
            this.url = url;
            this.title = title;
            this.statusCode = statusCode;
            this.allLinks = allLinks;
            this.jobLinks = jobLinks;
        }
    }

    public FetchedPage fetch(String targetUrl) {
        try {
            Connection connection = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36 (JobPortalCrawler/1.0)")
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
            Set<String> jobLinks = new LinkedHashSet<>();
            Elements anchors = doc.select("a[href]");
            for (Element a : anchors) {
                String href = a.attr("href");
                String text = a.text() != null ? a.text().trim() : "";
                String normalised = validator.resolveAndValidate(targetUrl, href);
                if (normalised == null) {
                    continue;
                }
                allLinks.add(normalised);
                if (isJobLink(normalised, text)) {
                    jobLinks.add(normalised);
                }
            }
            // Fallback for JS-rendered boards (Workday/Lever/Greenhouse expose JSON, not anchors):
            // harvest job-ish URLs embedded anywhere in the raw HTML.
            if (jobLinks.isEmpty()) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(https?://[^\\s\"'<>]+|/[^\\s\"'<>]*?(?:job|career|opening|position|posting|apply|vacancy|requisition|wd-|myworkday|greenhouse|lever|workable)[^\\s\"'<>]*)",
                                java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(doc.html());
                int guard = 0;
                while (m.find() && guard++ < 300) {
                    String cand = validator.resolveAndValidate(targetUrl, m.group(1));
                    if (cand != null && isJobLink(cand, "")) {
                        allLinks.add(cand);
                        jobLinks.add(cand);
                    }
                }
            }
            return new FetchedPage(targetUrl, title, status,
                    new ArrayList<>(allLinks), new ArrayList<>(jobLinks));
        } catch (Exception e) {
            log.warn("Fetch failed for {}: {}", targetUrl, e.getMessage());
            return new FetchedPage(targetUrl, "Fetch Failed: " + e.getMessage(), 503, List.of(), List.of());
        }
    }

    /**
     * Heuristic: job-posting hyperlinks usually contain job-ish path tokens or
     * an anchor text mentioning jobs/careers/apply/openings.
     */
    public boolean isJobLink(String url, String anchorText) {
        String u = url.toLowerCase();
        String t = anchorText != null ? anchorText.toLowerCase() : "";
        if (u.contains("/job") || u.contains("job-") || u.contains("jobid") || u.contains("job_id")
                || u.contains("jobs.") || u.contains("myworkdayjobs") || u.contains("wd-")
                || u.contains("greenhouse.io") || u.contains("lever.co") || u.contains("workable.com")
                || u.contains("ashbyhq.com") || u.contains("requisition") || u.contains("reqid")
                || u.contains("/career") || u.contains("/opening") || u.contains("/apply")
                || u.contains("position") || u.contains("vacancy") || u.contains("posting")) {
            return true;
        }
        return t.contains("job") || t.contains("career") || t.contains("apply")
                || t.contains("opening") || t.contains("vacancy") || t.contains("hiring")
                || t.contains("engineer") || t.contains("developer") || t.contains("analyst")
                || t.contains("designer") || t.contains("manager") || t.contains("intern");
    }
}
