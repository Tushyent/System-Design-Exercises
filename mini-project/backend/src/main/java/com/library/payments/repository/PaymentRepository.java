package com.library.payments.repository;

import com.library.payments.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findByMemberIdOrderByCreatedAtDesc(String memberId);
}
