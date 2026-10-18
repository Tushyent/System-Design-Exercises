package com.newsagg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.newsagg.config.CrawlerProperties;
import com.newsagg.config.RateLimiterProperties;

@SpringBootApplication
@EnableConfigurationProperties({RateLimiterProperties.class, CrawlerProperties.class})
public class NewsAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsAggregatorApplication.class, args);
    }
}
