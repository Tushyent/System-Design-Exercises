package com.library.payments.service;

import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.payments.dto.PayFineRequest;
import com.library.payments.dto.PaymentReceipt;
import com.library.payments.model.Payment;
import com.library.payments.model.PaymentStatus;
import com.library.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FineRepository fineRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * Idempotent Payment Processing.
     * Prevents double payment using client idempotency key.
     */
    public PaymentReceipt processPayment(PayFineRequest request, String memberId) {
        String idempotencyKey = request.getIdempotencyKey().trim();

        // 1. Idempotency Check: Return previously processed receipt if replayed
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment p = existingPayment.get();
            log.info("Idempotent replay detected for key {}. Returning existing receipt {}.", idempotencyKey, p.getPaymentId());
            return PaymentReceipt.builder()
                    .paymentId(p.getPaymentId())
                    .fineId(p.getFineId())
                    .memberId(p.getMemberId())
                    .amountPaid(p.getAmount())
                    .idempotencyKey(p.getIdempotencyKey())
                    .status(p.getStatus().name())
                    .transactionRef(p.getTransactionRef())
                    .timestamp(p.getCreatedAt())
                    .isDuplicateReplay(true)
                    .build();
        }

        // 2. Fetch Fine
        Fine fine = fineRepository.findByFineId(request.getFineId())
                .or(() -> fineRepository.findById(request.getFineId()))
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found: " + request.getFineId()));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new ConflictException("Fine " + fine.getFineId() + " has already been paid in full");
        }

        // Ownership enforcement: Ensure debtor is paying their own fine
        if (!fine.getMemberId().equals(memberId)) {
            throw new ConflictException("Unauthorized: Cannot pay fine belonging to another member");
        }

        if (Math.abs(fine.getAmount() - request.getAmount()) > 0.01) {
            throw new ConflictException(String.format("Payment amount (INR %.2f) does not match fine amount (INR %.2f)", request.getAmount(), fine.getAmount()));
        }

        // 3. Process simulated transaction
        String paymentId = idGenerator.nextPrefixedId("PAY");
        String txRef = "SIM-TX-" + idGenerator.nextIdString().substring(8);
        Instant now = Instant.now();

        Payment payment = Payment.builder()
                .id(idGenerator.nextIdString())
                .paymentId(paymentId)
                .fineId(fine.getFineId())
                .memberId(memberId)
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.SUCCESS)
                .transactionRef(txRef)
                .createdAt(now)
                .build();

        Payment saved = paymentRepository.save(payment);

        // 4. Update Fine status
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(now);
        fineRepository.save(fine);

        // 5. Cache Idempotency Token in Redis (24 hour TTL)
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("idempotency:pay:" + idempotencyKey, paymentId, Duration.ofHours(24));
            } catch (Exception e) {
                log.warn("Redis idempotency write failed: {}", e.getMessage());
            }
        }

        log.info("Simulated payment {} of INR {} processed for fine {}", paymentId, request.getAmount(), fine.getFineId());

        return PaymentReceipt.builder()
                .paymentId(paymentId)
                .fineId(fine.getFineId())
                .memberId(memberId)
                .amountPaid(saved.getAmount())
                .idempotencyKey(saved.getIdempotencyKey())
                .status(saved.getStatus().name())
                .transactionRef(saved.getTransactionRef())
                .timestamp(saved.getCreatedAt())
                .isDuplicateReplay(false)
                .build();
    }

    public List<Payment> getMemberPaymentHistory(String memberId) {
        return paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }
}
