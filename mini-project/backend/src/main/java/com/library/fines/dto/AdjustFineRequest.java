package com.library.fines.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustFineRequest {
    @DecimalMin(value = "0.0", message = "Amount cannot be negative")
    private double newAmount;

    @NotBlank(message = "Reason for adjustment is required")
    private String reason;
}
