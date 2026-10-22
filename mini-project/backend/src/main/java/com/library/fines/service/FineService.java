package com.library.fines.service;

import com.library.audit.service.AuditLogService;
import com.library.common.exception.ResourceNotFoundException;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;
    private final AuditLogService auditLogService;

    public List<Fine> getMemberFines(String memberId) {
        return fineRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    public Map<String, Object> getMemberFineSummary(String memberId) {
        List<Fine> unpaid = fineRepository.findByMemberIdAndStatus(memberId, FineStatus.UNPAID);
        double totalUnpaid = unpaid.stream().mapToDouble(Fine::getAmount).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("unpaidFines", unpaid);
        summary.put("totalUnpaidAmount", totalUnpaid);
        summary.put("unpaidCount", unpaid.size());
        return summary;
    }

    public List<Fine> getAllFines(FineStatus status) {
        if (status != null) {
            return fineRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return fineRepository.findAllByOrderByCreatedAtDesc();
    }

    public Fine waiveFine(String fineId, String reason, String staffEmail) {
        Fine fine = fineRepository.findByFineId(fineId)
                .or(() -> fineRepository.findById(fineId))
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found: " + fineId));

        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedBy(staffEmail);
        fine.setWaiverReason(reason);
        fine.setPaymentMethod("WAIVED");
        fine.setPaidAt(Instant.now());
        Fine saved = fineRepository.save(fine);

        auditLogService.record(staffEmail, "STAFF", "FINE_WAIVED", "FINE", fine.getFineId(),
                String.format("Waived fine INR %.2f for member %s. Reason: %s", fine.getAmount(), fine.getMemberId(), reason));
        return saved;
    }

    public Fine adjustFine(String fineId, double newAmount, String reason, String staffEmail) {
        Fine fine = fineRepository.findByFineId(fineId)
                .or(() -> fineRepository.findById(fineId))
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found: " + fineId));

        double oldAmount = fine.getAmount();
        fine.setAmount(newAmount);
        fine.setNotes("Amount adjusted from INR " + oldAmount + " to INR " + newAmount + ". Reason: " + reason);
        Fine saved = fineRepository.save(fine);

        auditLogService.record(staffEmail, "STAFF", "FINE_ADJUSTED", "FINE", fine.getFineId(),
                String.format("Adjusted fine INR %.2f -> INR %.2f for member %s. Reason: %s", oldAmount, newAmount, fine.getMemberId(), reason));
        return saved;
    }

    public Fine recordCashPayment(String fineId, String notes, String staffEmail) {
        Fine fine = fineRepository.findByFineId(fineId)
                .or(() -> fineRepository.findById(fineId))
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found: " + fineId));

        fine.setStatus(FineStatus.PAID);
        fine.setPaymentMethod("CASH_OFFLINE");
        fine.setPaidAt(Instant.now());
        fine.setNotes("Cash payment at desk: " + (notes != null ? notes : ""));
        Fine saved = fineRepository.save(fine);

        auditLogService.record(staffEmail, "STAFF", "CASH_PAYMENT", "FINE", fine.getFineId(),
                String.format("Recorded offline cash payment of INR %.2f for fine %s (Member: %s)", fine.getAmount(), fine.getFineId(), fine.getMemberId()));
        return saved;
    }
}
