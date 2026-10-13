package com.jobportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.jobportal.config.CrawlerProperties;
import com.jobportal.config.RateLimiterProperties;

@SpringBootApplication
@EnableConfigurationProperties({RateLimiterProperties.class, CrawlerProperties.class})
public class JobPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobPortalApplication.class, args);
    }
}
