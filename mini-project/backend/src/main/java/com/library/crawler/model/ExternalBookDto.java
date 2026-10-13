package com.library.crawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalBookDto {
    private String isbn;
    private String title;
    private List<String> authors;
    private List<String> categories;
    private String publisher;
    private int publicationYear;
    private String description;
    private String coverUrl;
    private Double externalRating;
    private String externalSource;
}
