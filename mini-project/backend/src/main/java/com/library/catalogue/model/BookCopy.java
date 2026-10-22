package com.library.catalogue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "book_copies")
@CompoundIndex(name = "book_status_idx", def = "{'bookId': 1, 'status': 1}")
public class BookCopy {

    @Id
    private String id;

    @Indexed
    private String bookId;

    @Indexed(unique = true)
    private String barcode;

    @Builder.Default
    private CopyStatus status = CopyStatus.AVAILABLE;

    private String currentLoanId;
    private String currentHolderId;
    private Instant pickupDeadline;

    @Builder.Default
    private String location = "Main Stacks - Central Library";
    private String conditionNotes;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
