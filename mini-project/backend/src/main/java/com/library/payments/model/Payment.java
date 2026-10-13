package com.library.payments.model;

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
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String paymentId;

    @Indexed
    private String fineId;

    @Indexed
    private String memberId;

    private double amount;

    @Indexed(unique = true)
    private String idempotencyKey;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.SUCCESS;

    private String transactionRef;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
