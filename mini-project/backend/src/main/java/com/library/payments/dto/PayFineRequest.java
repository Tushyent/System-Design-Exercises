package com.library.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayFineRequest {
    @NotBlank(message = "Fine ID is required")
    private String fineId;

    @Positive(message = "Payment amount must be greater than zero")
    private double amount;

    @NotBlank(message = "Idempotency key is required to prevent duplicate payments")
    private String idempotencyKey;
}
