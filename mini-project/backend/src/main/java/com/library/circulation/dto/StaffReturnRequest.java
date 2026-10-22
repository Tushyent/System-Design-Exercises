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
public class StaffReturnRequest {
    @NotBlank(message = "Copy Barcode is required")
    private String barcode;

    @Builder.Default
    private String condition = "NORMAL"; // NORMAL, DAMAGED, LOST

    private Double damageFee;
    private String staffNotes;
}
