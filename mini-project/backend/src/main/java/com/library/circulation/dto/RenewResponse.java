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
public class RenewResponse {

    private String loanId;
    private String bookTitle;
    private Instant newDueDate;
    private int renewalCount;
    private int maxRenewals;
    private String status;
    private String message;
}
