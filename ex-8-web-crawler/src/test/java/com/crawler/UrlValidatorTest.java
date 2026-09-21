package com.crawler;

import com.crawler.service.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UrlValidatorTest {

    private UrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UrlValidator();
    }

    @Test
    void testValidHttpUrls() {
        String result = validator.normalizeAndValidate("http://example.com/index.html", null);
        assertEquals("http://example.com/index.html", result);

        String result2 = validator.normalizeAndValidate("https://EXAMPLE.COM/about#team", null);
        assertEquals("https://example.com/about", result2); // host lowered, fragment stripped
    }

    @Test
    void testTrailingSlashRemoval() {
        String result = validator.normalizeAndValidate("http://localhost:8080/mock-web/", null);
        assertEquals("http://localhost:8080/mock-web", result);

        String root = validator.normalizeAndValidate("http://localhost:8080/", null);
        assertEquals("http://localhost:8080/", root); // root slash preserved
    }

    @Test
    void testDisallowedSchemes() {
        assertNull(validator.normalizeAndValidate("mailto:test@example.com", null));
        assertNull(validator.normalizeAndValidate("javascript:void(0)", null));
        assertNull(validator.normalizeAndValidate("ftp://ftp.example.com/file", null));
        assertNull(validator.normalizeAndValidate("#section1", null));
    }

    @Test
    void testDisallowedExtensions() {
        assertNull(validator.normalizeAndValidate("http://example.com/docs/paper.pdf", null));
        assertNull(validator.normalizeAndValidate("http://example.com/images/logo.png", null));
        assertNull(validator.normalizeAndValidate("http://example.com/build/archive.zip", null));
        assertNull(validator.normalizeAndValidate("http://example.com/style.css", null));
    }

    @Test
    void testDomainBoundaryRestriction() {
        List<String> allowed = Collections.singletonList("localhost");

        assertNotNull(validator.normalizeAndValidate("http://localhost:8080/page1.html", allowed));
        assertNull(validator.normalizeAndValidate("http://external-site.com/index.html", allowed));
    }

    @Test
    void testRelativeUrlResolution() {
        String base = "http://localhost:8080/mock-web/index.html";
        String resolved = validator.resolveAndValidate(base, "tech.html", null);
        assertEquals("http://localhost:8080/mock-web/tech.html", resolved);

        String rootResolved = validator.resolveAndValidate(base, "/sports.html", null);
        assertEquals("http://localhost:8080/sports.html", rootResolved);
    }
}
