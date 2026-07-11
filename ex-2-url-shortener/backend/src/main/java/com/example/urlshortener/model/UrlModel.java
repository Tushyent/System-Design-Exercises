package com.example.urlshortener.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "url_mapping")
public class UrlModel {
    @Id
    private String id;
    private String longUrl;
    private String shortCode;
    private String createdAt;

}
