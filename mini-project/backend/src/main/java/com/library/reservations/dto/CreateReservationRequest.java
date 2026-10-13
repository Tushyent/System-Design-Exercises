package com.library.reservations.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {
    @NotBlank(message = "Book ID is required")
    private String bookId;

    private String memberId; // If null, taken from principal
}
