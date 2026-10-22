package com.library.members.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatusUpdateRequest {
    @NotBlank(message = "Status is required (ACTIVE, SUSPENDED, INACTIVE)")
    private String status;

    private String reason;
    private Instant suspendedUntil;
}
