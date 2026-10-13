package com.library.shorturl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "short_urls")
public class ShortUrl {

    @Id
    private String id;

    @Indexed(unique = true)
    private String shortCode;

    private String targetUrl;
    private String resourceType; // "BOOK", "RESERVATION", "CATALOGUE"
    private String resourceId;

    @Builder.Default
    private long clickCount = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
