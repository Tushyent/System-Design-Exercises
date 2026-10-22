package com.library.fines.controller;

import com.library.common.dto.ApiResponse;
import com.library.fines.dto.AdjustFineRequest;
import com.library.fines.dto.CashPaymentRequest;
import com.library.fines.dto.WaiveFineRequest;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.service.FineService;
import com.library.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/my-fines")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyFines(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> summary = fineService.getMemberFineSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(summary, "Fines retrieved"));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Fine>>> getAllFines(
            @RequestParam(required = false) FineStatus status) {
        List<Fine> fines = fineService.getAllFines(status);
        return ResponseEntity.ok(ApiResponse.ok(fines, "All library fines retrieved"));
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Fine>> waiveFine(
            @PathVariable String id,
            @Valid @RequestBody WaiveFineRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Fine fine = fineService.waiveFine(id, request.getReason(), staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(fine, "Fine waived successfully"));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Fine>> adjustFine(
            @PathVariable String id,
            @Valid @RequestBody AdjustFineRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Fine fine = fineService.adjustFine(id, request.getNewAmount(), request.getReason(), staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(fine, "Fine amount adjusted successfully"));
    }

    @PostMapping("/{id}/cash-payment")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Fine>> recordCashPayment(
            @PathVariable String id,
            @RequestBody(required = false) CashPaymentRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        String notes = request != null ? request.getNotes() : null;
        Fine fine = fineService.recordCashPayment(id, notes, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(fine, "Cash payment recorded successfully"));
    }
}
