package com.jobportal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Job Portal — Job Discovery Crawler API")
                        .version("1.0.0")
                        .description("Spring Boot REST API for automatic job-posting discovery: "
                                + "BFS job-listing crawler (URL queue + visited set) protected by a "
                                + "Redis token-bucket rate limiter (capacity 5, refill 1/s).")
                        .contact(new Contact()
                                .name("System Design Lab")
                                .email("sdlab@ssn.edu.in")));
    }
}
