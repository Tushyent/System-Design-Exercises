package com.library.reservations.controller;

import com.library.common.dto.ApiResponse;
import com.library.reservations.dto.CreateReservationRequest;
import com.library.reservations.model.Reservation;
import com.library.reservations.service.ReservationService;
import com.library.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Reservation>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {

        String memberId = request.getMemberId();
        if (memberId == null && authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            memberId = principal.getId();
        }

        Reservation reservation = reservationService.createReservation(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reservation, "Book reserved successfully (Queue position: " + reservation.getQueuePosition() + ")"));
    }

    @GetMapping("/my-reservations")
    public ResponseEntity<ApiResponse<List<Reservation>>> getMyReservations(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<Reservation> list = reservationService.getMemberReservations(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(list, "Reservations retrieved"));
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<Reservation>> cancelReservation(
            @PathVariable String reservationId,
            Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Reservation cancelled = reservationService.cancelReservation(reservationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(cancelled, "Reservation cancelled"));
    }
}
