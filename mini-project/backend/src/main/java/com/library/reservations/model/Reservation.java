package com.library.reservations.model;

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
@Document(collection = "reservations")
@CompoundIndex(name = "book_queue_idx", def = "{'bookId': 1, 'status': 1, 'queuePosition': 1}")
public class Reservation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String reservationId;

    @Indexed
    private String bookId;

    private String bookTitle;

    @Indexed
    private String memberId;

    private String memberEmail;

    private int queuePosition;

    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    private String allocatedCopyId;
    private Instant readyAt;
    private Instant expiresAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
