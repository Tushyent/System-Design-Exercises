package com.jobportal.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Built-in "external job website": deterministic graph of job-listing pages
 * served by the same app so Postman demos work with zero internet.
 * Graph: /mock/jobs hub -> 6 postings + 2 categories; categories overlap the
 * hub links (dedup demo); postings link hub + 2 related (cycle demo).
 */
@RestController
@RequestMapping("/mock/jobs")
@CrossOrigin(origins = "*")
public class MockJobSiteController {

    private static String page(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + title
                + "</title></head><body><h1>Acme Careers — Mock Job Website</h1>"
                + "<nav><a href=\"/mock/jobs\">All Jobs</a> | "
                + "<a href=\"/mock/jobs/category/engineering\">Engineering</a> | "
                + "<a href=\"/mock/jobs/category/design\">Design</a></nav>"
                + "<h2>" + title + "</h2>" + body + "</body></html>";
    }

    private static String card(String id, String role) {
        return "<div><h3><a href=\"/mock/jobs/" + id + "\">" + role + "</a></h3>"
                + "<p><a href=\"/mock/jobs/" + id + "\">View posting & Apply</a></p></div>";
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String hub() {
        return page("All Job Openings",
                card("job-101", "Backend Engineer — Platform (Chennai)")
                        + card("job-102", "Frontend Developer — Web (Bengaluru)")
                        + card("job-103", "ML Engineer — AI (Remote)")
                        + card("job-104", "UI Designer — Design (Chennai)")
                        + card("job-105", "Data Analyst — Analytics (Hyderabad)")
                        + card("job-106", "DevOps Engineer — Platform (Remote)")
                        + "<ul><li><a href=\"/mock/jobs/category/engineering\">Engineering jobs</a></li>"
                        + "<li><a href=\"/mock/jobs/category/design\">Design jobs</a></li></ul>");
    }

    @GetMapping(value = "/category/engineering", produces = MediaType.TEXT_HTML_VALUE)
    public String engineering() {
        return page("Engineering Jobs",
                card("job-101", "Backend Engineer — Platform (Chennai)")
                        + card("job-103", "ML Engineer — AI (Remote)")
                        + card("job-106", "DevOps Engineer — Platform (Remote)"));
    }

    @GetMapping(value = "/category/design", produces = MediaType.TEXT_HTML_VALUE)
    public String design() {
        return page("Design Jobs",
                card("job-102", "Frontend Developer — Web (Bengaluru)")
                        + card("job-104", "UI Designer — Design (Chennai)"));
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String posting(@PathVariable String id) {
        java.util.Map<String, String> jobs = java.util.Map.of(
                "job-101", "Backend Engineer — Platform (Chennai) [Java, Spring Boot, Redis]",
                "job-102", "Frontend Developer — Web (Bengaluru) [React, TypeScript]",
                "job-103", "ML Engineer — AI (Remote) [Python, PyTorch]",
                "job-104", "UI Designer — Design (Chennai) [Figma, Design Systems]",
                "job-105", "Data Analyst — Analytics (Hyderabad) [SQL, Dashboards]",
                "job-106", "DevOps Engineer — Platform (Remote) [Docker, Kubernetes]");
        String j = jobs.get(id);
        if (j == null) {
            return page("Not found", "<p>No such posting: " + id + "</p>");
        }
        String[] ids = {"job-101", "job-102", "job-103", "job-104", "job-105", "job-106"};
        int i = java.util.Arrays.asList(ids).indexOf(id);
        String r1 = ids[(i + 1) % ids.length], r2 = ids[(i + 2) % ids.length];
        return page(j, "<p><a href=\"/mock/jobs/" + id + "/apply\">Apply for this job</a></p>"
                + "<ul><li><a href=\"/mock/jobs/" + r1 + "\">Related: " + r1 + "</a></li>"
                + "<li><a href=\"/mock/jobs/" + r2 + "\">Related: " + r2 + "</a></li>"
                + "<li><a href=\"/mock/jobs\">Back to all jobs</a></li></ul>");
    }

    @GetMapping(value = "/{id}/apply", produces = MediaType.TEXT_HTML_VALUE)
    public String apply(@PathVariable String id) {
        return page("Apply — " + id, "<p>Application form for " + id + " (demo leaf page).</p>"
                + "<p><a href=\"/mock/jobs/" + id + "\">Back to posting</a></p>");
    }
}
