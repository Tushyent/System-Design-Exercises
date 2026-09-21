package com.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private int depth;
    private String parentUrl;
    @Builder.Default
    private long discoveredTimestamp = System.currentTimeMillis();
}
