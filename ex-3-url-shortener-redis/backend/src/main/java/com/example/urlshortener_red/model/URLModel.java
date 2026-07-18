package com.example.urlshortener_red.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;


@Data
@Document(collection = "urls")
public class URLModel {

    @Id
    private String id;

    private String longUrl;

    private String shortCode;

}