package com.library.fines.service;

import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;

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
}
