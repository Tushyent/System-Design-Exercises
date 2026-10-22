package com.library.circulation.repository;

import com.library.circulation.model.Loan;
import com.library.circulation.model.LoanStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends MongoRepository<Loan, String> {
    Optional<Loan> findByLoanId(String loanId);
    Optional<Loan> findByCopyIdAndStatus(String copyId, LoanStatus status);
    List<Loan> findByMemberIdAndStatus(String memberId, LoanStatus status);
    List<Loan> findByMemberIdOrderByBorrowedAtDesc(String memberId);
    List<Loan> findByStatusOrderByBorrowedAtDesc(LoanStatus status);
    List<Loan> findAllByOrderByBorrowedAtDesc();
    List<Loan> findByBookTitleContainingIgnoreCaseOrBarcodeContainingIgnoreCase(String bookTitle, String barcode);
    int countByMemberIdAndStatus(String memberId, LoanStatus status);
}
