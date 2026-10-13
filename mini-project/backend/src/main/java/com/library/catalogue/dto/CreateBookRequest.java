package com.library.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    private List<String> authors;
    private List<String> categories;
    private String publisher;
    private int publicationYear;
    private String description;
    private String coverUrl;

    @Builder.Default
    private int initialCopies = 1;
}
