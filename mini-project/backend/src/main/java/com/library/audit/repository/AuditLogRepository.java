package com.library.audit.repository;

import com.library.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<AuditLog> findTop50ByOrderByTimestampDesc();
    List<AuditLog> findByStaffEmailOrderByTimestampDesc(String staffEmail);
    List<AuditLog> findByTargetResourceOrderByTimestampDesc(String targetResource);
}
