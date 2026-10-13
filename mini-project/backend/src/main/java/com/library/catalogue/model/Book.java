package com.library.catalogue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
public class Book {

    @Id
    private String id;

    @Indexed
    private String isbn;

    @TextIndexed(weight = 3)
    @Indexed
    private String title;

    @TextIndexed(weight = 2)
    @Builder.Default
    private List<String> authors = new ArrayList<>();

    @Indexed
    @Builder.Default
    private List<String> categories = new ArrayList<>();

    private String publisher;
    private int publicationYear;
    private String description;
    private String coverUrl;

    // Enriched external metadata (e.g. from Web Crawler)
    private Double externalRating;
    private String externalSource;

    @Builder.Default
    private int totalCopies = 0;

    @Builder.Default
    private int availableCopies = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
