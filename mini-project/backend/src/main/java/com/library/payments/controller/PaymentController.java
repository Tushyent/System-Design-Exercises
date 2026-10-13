package com.library.payments.controller;

import com.library.common.dto.ApiResponse;
import com.library.payments.dto.PayFineRequest;
import com.library.payments.dto.PaymentReceipt;
import com.library.payments.model.Payment;
import com.library.payments.service.PaymentService;
import com.library.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<PaymentReceipt>> payFine(
            @Valid @RequestBody PayFineRequest request,
            Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        PaymentReceipt receipt = paymentService.processPayment(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(receipt, receipt.isDuplicateReplay() ? "Idempotent payment replayed" : "Payment processed successfully"));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<Payment>>> getMyPaymentHistory(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<Payment> history = paymentService.getMemberPaymentHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(history, "Payment history retrieved"));
    }
}
