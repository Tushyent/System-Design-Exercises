package com.library.fines.repository;

import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends MongoRepository<Fine, String> {
    Optional<Fine> findByFineId(String fineId);
    Optional<Fine> findByLoanId(String loanId);
    List<Fine> findByMemberIdAndStatus(String memberId, FineStatus status);
    List<Fine> findByMemberIdOrderByCreatedAtDesc(String memberId);
    List<Fine> findByStatusOrderByCreatedAtDesc(FineStatus status);
    List<Fine> findAllByOrderByCreatedAtDesc();
}
