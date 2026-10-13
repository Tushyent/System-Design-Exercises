package com.newsagg.dto;

import java.util.List;

/** One fetched page inside a crawl response. */
public record PageResult(
        String url,
        String title,
        int statusCode,
        List<String> articleLinksFound,
        boolean fromCache) {
}
