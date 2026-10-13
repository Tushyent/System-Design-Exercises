package com.library.circulation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {
    private String loanId;
    private String copyId;
    private String status;
    private Instant returnedAt;
    private int overdueDays;
    private double fineAmount;
    private String fineId;
    private String assignedToReservationId;
    private String message;
}
