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
public class BorrowRequest {
    @NotBlank(message = "Book ID is required")
    private String bookId;

    private String memberId; // If null, extracted from security context
}
