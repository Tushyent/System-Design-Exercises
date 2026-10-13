package com.library.payments;

import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.payments.dto.PayFineRequest;
import com.library.payments.dto.PaymentReceipt;
import com.library.payments.repository.PaymentRepository;
import com.library.payments.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdempotentPaymentTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Test
    @DisplayName("Should process payment safely and return identical receipt on duplicate request with same idempotency key")
    void testIdempotentPaymentProcessing() {
        String fineId = idGenerator.nextPrefixedId("FINE");
        String memberId = "MEM-TEST-99";
        double fineAmount = 15.00;

        Fine fine = Fine.builder()
                .id(idGenerator.nextIdString())
                .fineId(fineId)
                .loanId("LOAN-TEST-99")
                .memberId(memberId)
                .overdueDays(10)
                .ratePerDay(1.50)
                .amount(fineAmount)
                .status(FineStatus.UNPAID)
                .build();
        fineRepository.save(fine);

        String idempotencyKey = "PAY-KEY-" + UUID.randomUUID();

        PayFineRequest request = PayFineRequest.builder()
                .fineId(fineId)
                .amount(fineAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        // First payment call
        PaymentReceipt firstReceipt = paymentService.processPayment(request, memberId);
        assertNotNull(firstReceipt);
        assertEquals("SUCCESS", firstReceipt.getStatus());
        assertFalse(firstReceipt.isDuplicateReplay());
        assertEquals(fineAmount, firstReceipt.getAmountPaid());

        // Verify Fine updated to PAID
        Fine fineAfter = fineRepository.findByFineId(fineId).orElseThrow();
        assertEquals(FineStatus.PAID, fineAfter.getStatus());

        // Second payment call with IDENTICAL idempotency key
        PaymentReceipt duplicateReceipt = paymentService.processPayment(request, memberId);
        assertNotNull(duplicateReceipt);
        assertTrue(duplicateReceipt.isDuplicateReplay(), "Second call must be flagged as idempotent duplicate replay");
        assertEquals(firstReceipt.getPaymentId(), duplicateReceipt.getPaymentId(), "Payment ID must match original receipt");
        assertEquals(firstReceipt.getTransactionRef(), duplicateReceipt.getTransactionRef(), "Transaction Ref must match original receipt");

        // Verify exactly 1 payment document was saved in database
        long count = paymentRepository.findAll().stream()
                .filter(p -> p.getIdempotencyKey().equals(idempotencyKey))
                .count();
        assertEquals(1, count, "Only ONE payment record must exist for the idempotency key");
    }

    @Test
    @DisplayName("Should reject payment if authenticated member does not own the fine")
    void testFineDebtorOwnershipEnforced() {
        String fineId = idGenerator.nextPrefixedId("FINE");
        String legitimateMember = "MEM-OWNER-42";
        String attackerMember = "MEM-ATTACKER-99";
        double fineAmount = 10.00;

        Fine fine = Fine.builder()
                .id(idGenerator.nextIdString())
                .fineId(fineId)
                .loanId("LOAN-TEST-42")
                .memberId(legitimateMember)
                .overdueDays(5)
                .ratePerDay(2.00)
                .amount(fineAmount)
                .status(FineStatus.UNPAID)
                .build();
        fineRepository.save(fine);

        PayFineRequest request = PayFineRequest.builder()
                .fineId(fineId)
                .amount(fineAmount)
                .idempotencyKey("PAY-HIJACK-" + UUID.randomUUID())
                .build();

        com.library.common.exception.ConflictException ex = assertThrows(
                com.library.common.exception.ConflictException.class,
                () -> paymentService.processPayment(request, attackerMember)
        );
        assertTrue(ex.getMessage().contains("Unauthorized: Cannot pay fine belonging to another member"));
    }
}
