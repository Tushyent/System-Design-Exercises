package com.newsagg.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Built-in "external news website": deterministic graph of news pages served
 * by the same app so Postman demos work with zero internet.
 * Graph: /mock/news front page -> 6 articles + 2 section pages; sections
 * overlap the front-page links (dedup demo); articles link front page +
 * 2 related stories (cycle demo).
 */
@RestController
@RequestMapping("/mock/news")
@CrossOrigin(origins = "*")
public class MockNewsSiteController {

    private static String page(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + title
                + "</title></head><body><h1>Daily Byte - Mock News Website</h1>"
                + "<nav><a href=\"/mock/news\">Front Page</a> | "
                + "<a href=\"/mock/news/section/technology\">Technology</a> | "
                + "<a href=\"/mock/news/section/sports\">Sports</a></nav>"
                + "<h2>" + title + "</h2>" + body + "</body></html>";
    }

    private static String card(String id, String headline) {
        return "<article><h3><a href=\"/mock/news/" + id + "\">" + headline + "</a></h3>"
                + "<p><a href=\"/mock/news/" + id + "\">Read full story</a></p></article>";
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String frontPage() {
        return page("Front Page - Top Stories",
                card("story-ai-chips", "AI Chip Race Heats Up as Startups Ship New Silicon")
                        + card("story-monsoon", "Monsoon Session: Parliament Clears Digital Bill")
                        + card("story-cricket", "Cricket World Cup Final Sets Viewership Record")
                        + card("story-markets", "Markets Rally as Inflation Cools")
                        + card("story-space", "ISRO Unveils Reusable Rocket Prototype")
                        + card("story-tennis", "Teen Sensation Wins Maiden Grand Slam Title")
                        + "<ul><li><a href=\"/mock/news/section/technology\">Technology section</a></li>"
                        + "<li><a href=\"/mock/news/section/sports\">Sports section</a></li></ul>");
    }

    @GetMapping(value = "/section/technology", produces = MediaType.TEXT_HTML_VALUE)
    public String technology() {
        return page("Technology News",
                card("story-ai-chips", "AI Chip Race Heats Up as Startups Ship New Silicon")
                        + card("story-space", "ISRO Unveils Reusable Rocket Prototype")
                        + card("story-markets", "Markets Rally as Inflation Cools"));
    }

    @GetMapping(value = "/section/sports", produces = MediaType.TEXT_HTML_VALUE)
    public String sports() {
        return page("Sports News",
                card("story-cricket", "Cricket World Cup Final Sets Viewership Record")
                        + card("story-tennis", "Teen Sensation Wins Maiden Grand Slam Title"));
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String article(@PathVariable String id) {
        java.util.Map<String, String> stories = java.util.Map.of(
                "story-ai-chips", "AI Chip Race Heats Up [Technology, 5 min read]",
                "story-monsoon", "Monsoon Session: Parliament Clears Digital Bill [Politics]",
                "story-cricket", "Cricket World Cup Final Sets Viewership Record [Sports]",
                "story-markets", "Markets Rally as Inflation Cools [Business]",
                "story-space", "ISRO Unveils Reusable Rocket Prototype [Science]",
                "story-tennis", "Teen Sensation Wins Maiden Grand Slam Title [Sports]");
        String s = stories.get(id);
        if (s == null) {
            return page("Not found", "<p>No such story: " + id + "</p>");
        }
        String[] ids = {"story-ai-chips", "story-monsoon", "story-cricket",
                "story-markets", "story-space", "story-tennis"};
        int i = java.util.Arrays.asList(ids).indexOf(id);
        String r1 = ids[(i + 1) % ids.length], r2 = ids[(i + 2) % ids.length];
        return page(s, "<p>Full article text for " + id + " (demo).</p>"
                + "<ul><li><a href=\"/mock/news/" + r1 + "\">Related: " + r1 + "</a></li>"
                + "<li><a href=\"/mock/news/" + r2 + "\">Related: " + r2 + "</a></li>"
                + "<li><a href=\"/mock/news\">Back to front page</a></li></ul>");
    }
}
