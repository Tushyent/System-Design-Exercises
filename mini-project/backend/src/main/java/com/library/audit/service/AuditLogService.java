package com.library.audit.service;

import com.library.audit.model.AuditLog;
import com.library.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog record(String staffEmail, String staffRole, String action, String targetResource, String targetId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .staffEmail(staffEmail != null ? staffEmail : "system")
                .staffRole(staffRole != null ? staffRole : "STAFF")
                .action(action)
                .targetResource(targetResource)
                .targetId(targetId)
                .details(details)
                .timestamp(Instant.now())
                .build();
        try {
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
            return auditLog;
        }
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public Page<AuditLog> getPagedLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }
}
