package com.library.circulation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCheckoutRequest {
    @NotBlank(message = "Member identifier (Email, Member ID, or UUID) is required")
    private String memberIdentifier;

    @NotBlank(message = "Copy Barcode is required")
    private String barcode;

    @Builder.Default
    private int loanDays = 14;

    @Builder.Default
    private boolean overrideLimit = false;

    private String overrideReason;
}
