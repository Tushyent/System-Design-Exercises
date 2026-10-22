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

    @PostMapping("/staff-checkout")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<BorrowResponse>> staffCheckout(
            @Valid @RequestBody com.library.circulation.dto.StaffCheckoutRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        BorrowResponse response = circulationService.staffCheckout(request, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(response, "Staff checkout completed"));
    }

    @PostMapping("/staff-return")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReturnResponse>> staffReturn(
            @Valid @RequestBody com.library.circulation.dto.StaffReturnRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        ReturnResponse response = circulationService.staffReturn(request, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(response, "Staff return processed"));
    }

    @GetMapping("/all-loans")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Loan>>> getAllLoans(
            @RequestParam(required = false) com.library.circulation.model.LoanStatus status,
            @RequestParam(required = false) String search) {
        List<Loan> loans = circulationService.getAllLoans(status, search);
        return ResponseEntity.ok(ApiResponse.ok(loans, "All circulation loans retrieved"));
    }
}

