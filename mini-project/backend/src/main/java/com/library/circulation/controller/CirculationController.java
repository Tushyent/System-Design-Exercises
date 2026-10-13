package com.library.circulation.controller;

import com.library.circulation.dto.BorrowRequest;
import com.library.circulation.dto.BorrowResponse;
import com.library.circulation.dto.RenewRequest;
import com.library.circulation.dto.RenewResponse;
import com.library.circulation.dto.ReturnRequest;
import com.library.circulation.dto.ReturnResponse;
import com.library.circulation.model.Loan;
import com.library.circulation.service.CirculationService;
import com.library.common.dto.ApiResponse;
import com.library.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circulation")
@RequiredArgsConstructor
public class CirculationController {

    private final CirculationService circulationService;

    @PostMapping("/borrow")
    public ResponseEntity<ApiResponse<BorrowResponse>> borrowBook(
            @Valid @RequestBody BorrowRequest request,
            Authentication authentication) {

        String memberId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean isStaff = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
            if (isStaff && request.getMemberId() != null && !request.getMemberId().isBlank()) {
                memberId = request.getMemberId();
            } else {
                memberId = principal.getId();
            }
        }

        BorrowResponse response = circulationService.borrowBook(request, memberId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Book borrowed successfully"));
    }

    @PostMapping("/renew")
    public ResponseEntity<ApiResponse<RenewResponse>> renewLoan(
            @Valid @RequestBody RenewRequest request,
            Authentication authentication) {

        String memberId = null;
        boolean isStaff = false;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            isStaff = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
            memberId = principal.getId();
        }

        RenewResponse response = circulationService.renewLoan(request, memberId, isStaff);
        return ResponseEntity.ok(ApiResponse.ok(response, "Loan renewed successfully"));
    }

    @PostMapping("/return")
    public ResponseEntity<ApiResponse<ReturnResponse>> returnBook(
            @Valid @RequestBody ReturnRequest request) {

        ReturnResponse response = circulationService.returnBook(request.getCopyIdOrBarcode());
        return ResponseEntity.ok(ApiResponse.ok(response, "Return processed"));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<ApiResponse<List<Loan>>> getMyActiveLoans(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<Loan> loans = circulationService.getMemberActiveLoans(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(loans, "Active loans retrieved"));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<Loan>>> getMyLoanHistory(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<Loan> history = circulationService.getMemberLoanHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(history, "Loan history retrieved"));
    }
}

