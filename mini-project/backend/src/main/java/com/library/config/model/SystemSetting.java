package com.library.config.model;

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
@Document(collection = "system_settings")
public class SystemSetting {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private String value;
    private String description;

    @Builder.Default
    private Instant updatedAt = Instant.now();

    private String updatedBy;
}
