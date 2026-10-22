package com.library.fines.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaiveFineRequest {
    @NotBlank(message = "Reason for waiver is required")
    private String reason;
}
