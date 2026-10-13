package com.library.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceipt {
    private String paymentId;
    private String fineId;
    private String memberId;
    private double amountPaid;
    private String idempotencyKey;
    private String status;
    private String transactionRef;
    private Instant timestamp;
    private boolean isDuplicateReplay;
}
