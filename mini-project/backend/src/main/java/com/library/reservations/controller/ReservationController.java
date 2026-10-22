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

    @GetMapping("/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Reservation>>> getAllReservations(
            @RequestParam(required = false) com.library.reservations.model.ReservationStatus status) {
        List<Reservation> list = reservationService.getAllReservations(status);
        return ResponseEntity.ok(ApiResponse.ok(list, "All library reservations retrieved"));
    }

    @PostMapping("/{reservationId}/fulfill")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Reservation>> fulfillReservation(
            @PathVariable String reservationId,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Reservation fulfilled = reservationService.fulfillReservation(reservationId, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(fulfilled, "Reservation fulfilled"));
    }

    @PostMapping("/{reservationId}/cancel-by-staff")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Reservation>> cancelReservationByStaff(
            @PathVariable String reservationId,
            @RequestParam(defaultValue = "Cancelled by library staff") String reason,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Reservation cancelled = reservationService.cancelReservationByStaff(reservationId, reason, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(cancelled, "Reservation cancelled by staff"));
    }

    @PostMapping("/{reservationId}/expire")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Reservation>> expireReservation(
            @PathVariable String reservationId,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Reservation expired = reservationService.expireReservation(reservationId, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(expired, "Unclaimed hold expired"));
    }
}
