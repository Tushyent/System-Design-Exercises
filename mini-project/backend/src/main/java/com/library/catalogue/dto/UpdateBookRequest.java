package com.library.catalogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequest {
    private String title;
    private String isbn;
    private List<String> authors;
    private List<String> categories;
    private String publisher;
    private Integer publicationYear;
    private String description;
    private String coverUrl;
    private Boolean archived;
}
