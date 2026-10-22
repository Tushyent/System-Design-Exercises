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
public class BorrowResponse {
    private String loanId;
    private String bookId;
    private String bookTitle;
    private String copyId;
    private String barcode;
    private Instant borrowedAt;
    private Instant dueDate;
    private String memberId;
    private String message;
}
