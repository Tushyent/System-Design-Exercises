package com.crawler.config;

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
                        .title("Distributed Web Crawler REST API")
                        .version("1.0.0")
                        .description("API documentation for Lab Exercise 8: Design and Develop a Web Crawler "
                                + "for Automated Web Content Discovery using Spring Boot and Redis.")
                        .contact(new Contact()
                                .name("SSN System Design Lab Group")
                                .email("sdlab@ssn.edu.in")));
    }
}
