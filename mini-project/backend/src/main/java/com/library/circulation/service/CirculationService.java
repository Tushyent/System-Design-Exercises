package com.library.circulation.service;

import com.library.catalogue.model.Book;
import com.library.catalogue.model.BookCopy;
import com.library.catalogue.model.CopyStatus;
import com.library.catalogue.repository.BookCopyRepository;
import com.library.catalogue.repository.BookRepository;
import com.library.catalogue.service.BookService;
import com.library.audit.service.AuditLogService;
import com.library.circulation.dto.StaffCheckoutRequest;
import com.library.circulation.dto.StaffReturnRequest;
import com.library.circulation.dto.BorrowRequest;
import com.library.circulation.dto.BorrowResponse;
import com.library.circulation.dto.ReturnResponse;
import com.library.circulation.dto.RenewRequest;
import com.library.circulation.dto.RenewResponse;
import com.library.circulation.model.Loan;
import com.library.circulation.model.LoanStatus;
import com.library.circulation.repository.LoanRepository;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.model.Member;
import com.library.members.repository.MemberRepository;
import com.library.reservations.model.Reservation;
import com.library.reservations.model.ReservationStatus;
import com.library.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CirculationService {

    private final MongoTemplate mongoTemplate;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final BookService bookService;
    private final AuditLogService auditLogService;

    /**
     * Concurrency-Safe Atomic Borrowing Workflow.
     * Uses atomic MongoDB findAndModify on BookCopy collection.
     * Exactly one thread can transition a copy from AVAILABLE -> BORROWED.
     */
    public BorrowResponse borrowBook(BorrowRequest request, String memberId) {
        // 1. Verify Member eligibility
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with ID: " + memberId));

        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            throw new ConflictException("Member account is " + member.getStatus() + ". Cannot borrow books.");
        }

        // 2. Check active loans limit
        int activeLoans = loanRepository.countByMemberIdAndStatus(memberId, LoanStatus.ACTIVE);
        if (activeLoans >= member.getMaxBorrowLimit()) {
            throw new ConflictException("Borrowing limit reached (" + member.getMaxBorrowLimit() + " books). Please return a book first.");
        }

        // 3. Check for severe unpaid fines (> INR 20.00)
        List<Fine> unpaidFines = fineRepository.findByMemberIdAndStatus(memberId, FineStatus.UNPAID);
        double totalUnpaid = unpaidFines.stream().mapToDouble(Fine::getAmount).sum();
        if (totalUnpaid > 20.00) {
            throw new ConflictException(String.format("Unpaid fines (INR %.2f) exceed allowed limit (INR 20.00). Please clear dues before borrowing.", totalUnpaid));
        }

        // 4. Verify book title exists
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + request.getBookId()));

        String loanId = idGenerator.nextPrefixedId("LOAN");

        // 5. ATOMIC CLAIM: Find any available copy and claim it atomically
        Query query = new Query(Criteria.where("bookId").is(request.getBookId())
                .and("status").is(CopyStatus.AVAILABLE));

        Update update = new Update()
                .set("status", CopyStatus.BORROWED)
                .set("currentHolderId", memberId)
                .set("currentLoanId", loanId);

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        BookCopy claimedCopy = mongoTemplate.findAndModify(query, update, options, BookCopy.class);

        if (claimedCopy == null) {
            // Contention or no available copies
            throw new ConflictException("All physical copies of '" + book.getTitle() + "' are currently checked out. You can reserve this book.");
        }

        // 6. Create Loan record
        Instant now = Instant.now();
        Instant dueDate = now.plus(Duration.ofDays(14));

        Loan loan = Loan.builder()
                .id(idGenerator.nextIdString())
                .loanId(loanId)
                .memberId(memberId)
                .memberCode(member.getMemberId())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .copyId(claimedCopy.getId())
                .barcode(claimedCopy.getBarcode())
                .borrowedAt(now)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .build();

        loanRepository.save(loan);

        // 7. Update available copy count in Book & Invalidate Cache
        int remainingAvailable = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
        book.setAvailableCopies(remainingAvailable);
        bookRepository.save(book);
        bookService.invalidateAvailabilityCache(book.getId());

        log.info("Member {} successfully borrowed copy {} (barcode {}) for book '{}'",
                memberId, claimedCopy.getId(), claimedCopy.getBarcode(), book.getTitle());

        return BorrowResponse.builder()
                .loanId(loanId)
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .copyId(claimedCopy.getId())
                .barcode(claimedCopy.getBarcode())
                .borrowedAt(now)
                .dueDate(dueDate)
                .memberId(memberId)
                .build();
    }

    /**
     * Idempotent Return Workflow with Overdue Fine Calculation and Reservation Queue Handover.
     */
    public ReturnResponse returnBook(String copyIdOrBarcode) {
        // Resolve copy by ID or Barcode
        BookCopy copy = copyRepository.findById(copyIdOrBarcode)
                .or(() -> copyRepository.findByBarcode(copyIdOrBarcode))
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found: " + copyIdOrBarcode));

        // Find associated active loan
        Loan loan = loanRepository.findByCopyIdAndStatus(copy.getId(), LoanStatus.ACTIVE)
                .orElse(null);

        if (loan == null) {
            // Idempotency: If copy is already returned
            if (copy.getStatus() == CopyStatus.AVAILABLE) {
                return ReturnResponse.builder()
                        .copyId(copy.getId())
                        .status("ALREADY_RETURNED")
                        .message("Book copy is already returned and available in library.")
                        .build();
            }
            throw new ConflictException("No active loan found for copy: " + copy.getBarcode());
        }

        Instant returnedAt = Instant.now();
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(returnedAt);

        // Calculate Overdue Fine (INR 1.50 per day, max INR 50.00)
        int overdueDays = 0;
        double fineAmount = 0.0;
        String fineId = null;

        if (returnedAt.isAfter(loan.getDueDate())) {
            overdueDays = (int) ChronoUnit.DAYS.between(loan.getDueDate(), returnedAt);
            if (overdueDays > 0) {
                fineAmount = Math.min(50.00, overdueDays * 1.50);
                fineId = idGenerator.nextPrefixedId("FINE");

                Fine fine = Fine.builder()
                        .id(idGenerator.nextIdString())
                        .fineId(fineId)
                        .loanId(loan.getLoanId())
                        .memberId(loan.getMemberId())
                        .overdueDays(overdueDays)
                        .ratePerDay(1.50)
                        .amount(fineAmount)
                        .status(FineStatus.UNPAID)
                        .createdAt(returnedAt)
                        .build();
                fineRepository.save(fine);
                loan.setFineCalculated(true);
            }
        }
        loanRepository.save(loan);

        // Check Reservation Queue (FIFO order)
        String assignedReservationId = null;
        Reservation pendingReservation = reservationRepository
                .findFirstByBookIdAndStatusOrderByQueuePositionAsc(copy.getBookId(), ReservationStatus.PENDING)
                .orElse(null);

        if (pendingReservation != null) {
            // Handover copy to first waiting reservation
            assignedReservationId = pendingReservation.getReservationId();
            pendingReservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
            pendingReservation.setAllocatedCopyId(copy.getId());
            pendingReservation.setReadyAt(returnedAt);
            pendingReservation.setExpiresAt(returnedAt.plus(Duration.ofHours(48)));
            reservationRepository.save(pendingReservation);

            copy.setStatus(CopyStatus.RESERVED_FOR_PICKUP);
            copy.setCurrentHolderId(pendingReservation.getMemberId());
            copy.setCurrentLoanId(null);
            copy.setPickupDeadline(pendingReservation.getExpiresAt());
            copyRepository.save(copy);

            log.info("Returned copy {} allocated to reservation {}", copy.getId(), assignedReservationId);
        } else {
            // No waiting reservations -> return to available shelf
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setCurrentHolderId(null);
            copy.setCurrentLoanId(null);
            copy.setPickupDeadline(null);
            copyRepository.save(copy);
        }

        // Update book available copies count & invalidate cache
        Book book = bookRepository.findById(copy.getBookId()).orElse(null);
        if (book != null) {
            int newAvailable = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
            book.setAvailableCopies(newAvailable);
            bookRepository.save(book);
            bookService.invalidateAvailabilityCache(book.getId());
        }

        return ReturnResponse.builder()
                .loanId(loan.getLoanId())
                .copyId(copy.getId())
                .status("RETURNED")
                .returnedAt(returnedAt)
                .overdueDays(overdueDays)
                .fineAmount(fineAmount)
                .fineId(fineId)
                .assignedToReservationId(assignedReservationId)
                .message("Copy returned successfully" + (overdueDays > 0 ? " (Overdue fine accrued)" : ""))
                .build();
    }

    public List<Loan> getMemberActiveLoans(String memberId) {
        return loanRepository.findByMemberIdAndStatus(memberId, LoanStatus.ACTIVE);
    }

    public List<Loan> getMemberLoanHistory(String memberId) {
        return loanRepository.findByMemberIdOrderByBorrowedAtDesc(memberId);
    }

    public static final int MAX_RENEWALS = 2;
    public static final int RENEWAL_EXTENSION_DAYS = 14;

    /**
     * Authoritative Loan Renewal Workflow.
     * Enforces loan ownership, ACTIVE status check, max renewal cap (2 renewals),
     * pending reservation queue defense, and atomic findAndModify concurrency protection.
     */
    public RenewResponse renewLoan(RenewRequest request, String memberId, boolean isStaff) {
        String loanId = request.getLoanId();
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan record not found with ID: " + loanId));

        // 1. Verify Ownership (unless Librarian or Admin)
        if (!isStaff && !loan.getMemberId().equals(memberId)) {
            throw new ConflictException("Unauthorized: This loan belongs to another member account.");
        }

        // 2. Loan must be in ACTIVE status
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ConflictException("Cannot renew: Loan is already marked as " + loan.getStatus() + ".");
        }

        // 3. Enforce maximum renewal limit (Max 2 renewals)
        if (loan.getRenewalCount() >= MAX_RENEWALS) {
            throw new ConflictException("Renewal limit reached: A maximum of " + MAX_RENEWALS + " renewals is permitted per loan.");
        }

        // 4. Check if book has a pending reservation waitlist
        int pendingReservations = reservationRepository.countByBookIdAndStatus(loan.getBookId(), ReservationStatus.PENDING);
        if (pendingReservations > 0) {
            throw new ConflictException("Cannot renew: " + pendingReservations + " member(s) are currently waiting in the reservation queue for this title.");
        }

        // 5. Calculate new due date (extends dueDate by 14 days from current dueDate or current instant)
        Instant baseTime = loan.getDueDate().isAfter(Instant.now()) ? loan.getDueDate() : Instant.now();
        Instant newDueDate = baseTime.plus(Duration.ofDays(RENEWAL_EXTENSION_DAYS));

        // 6. Concurrency-safe atomic update checking renewalCount
        Query query = new Query(Criteria.where("loanId").is(loanId)
                .and("status").is(LoanStatus.ACTIVE)
                .and("renewalCount").is(loan.getRenewalCount()));

        Update update = new Update()
                .set("dueDate", newDueDate)
                .inc("renewalCount", 1)
                .set("lastRenewedAt", Instant.now());

        Loan updatedLoan = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Loan.class
        );

        if (updatedLoan == null) {
            throw new ConflictException("Concurrent renewal or loan state change detected. Please retry.");
        }

        log.info("Loan {} renewed successfully by member {} (Count: {}/{}, New Due Date: {})",
                loanId, memberId, updatedLoan.getRenewalCount(), MAX_RENEWALS, newDueDate);

        return RenewResponse.builder()
                .loanId(loanId)
                .bookTitle(updatedLoan.getBookTitle())
                .newDueDate(newDueDate)
                .renewalCount(updatedLoan.getRenewalCount())
                .maxRenewals(MAX_RENEWALS)
                .status("RENEWED")
                .message("Loan successfully renewed! Extended by " + RENEWAL_EXTENSION_DAYS + " days until " + newDueDate)
                .build();
    }

    public BorrowResponse staffCheckout(StaffCheckoutRequest request, String staffEmail) {
        Member member = memberRepository.findById(request.getMemberIdentifier())
                .or(() -> memberRepository.findByEmail(request.getMemberIdentifier()))
                .or(() -> memberRepository.findByMemberId(request.getMemberIdentifier()))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.getMemberIdentifier()));

        // Check eligibility
        if (!request.isOverrideLimit()) {
            if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                throw new ConflictException("Member account status is " + member.getStatus() + ". Checkout blocked.");
            }
            int activeLoans = loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
            if (activeLoans >= member.getMaxBorrowLimit()) {
                throw new ConflictException("Borrow limit reached (" + member.getMaxBorrowLimit() + "). Use staff override if authorized.");
            }
            List<Fine> unpaidFines = fineRepository.findByMemberIdAndStatus(member.getId(), FineStatus.UNPAID);
            double totalUnpaid = unpaidFines.stream().mapToDouble(Fine::getAmount).sum();
            if (totalUnpaid > 20.00) {
                throw new ConflictException(String.format("Unpaid fines (INR %.2f) exceed INR 20.00 limit. Use staff override if authorized.", totalUnpaid));
            }
        }

        // Find Copy by Barcode
        BookCopy copy = copyRepository.findByBarcode(request.getBarcode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found with barcode: " + request.getBarcode()));

        if (copy.getStatus() == CopyStatus.BORROWED) {
            throw new ConflictException("Copy barcode " + request.getBarcode() + " is currently on loan to another member!");
        }
        if (copy.getStatus() != CopyStatus.AVAILABLE && copy.getStatus() != CopyStatus.RESERVED_FOR_PICKUP) {
            throw new ConflictException("Copy cannot be checked out in current status: " + copy.getStatus());
        }
        if (copy.getStatus() == CopyStatus.RESERVED_FOR_PICKUP && copy.getCurrentHolderId() != null
                && !copy.getCurrentHolderId().equals(member.getId()) && !request.isOverrideLimit()) {
            throw new ConflictException("Copy is reserved for pickup by another member.");
        }

        Book book = bookRepository.findById(copy.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found for copy: " + copy.getBookId()));

        String loanId = idGenerator.nextPrefixedId("LOAN");
        Instant now = Instant.now();
        int days = request.getLoanDays() > 0 ? request.getLoanDays() : 14;
        Instant dueDate = now.plus(Duration.ofDays(days));

        // Update Copy
        copy.setStatus(CopyStatus.BORROWED);
        copy.setCurrentHolderId(member.getId());
        copy.setCurrentLoanId(loanId);
        copy.setPickupDeadline(null);
        copyRepository.save(copy);

        // Create Loan
        Loan loan = Loan.builder()
                .id(idGenerator.nextIdString())
                .loanId(loanId)
                .memberId(member.getId())
                .memberCode(member.getMemberId())
                .bookId(copy.getBookId())
                .bookTitle(book.getTitle())
                .copyId(copy.getId())
                .barcode(copy.getBarcode())
                .borrowedAt(now)
                .dueDate(dueDate)
                .status(LoanStatus.ACTIVE)
                .build();
        loanRepository.save(loan);

        // Update Book Available Count
        int newAvailable = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
        book.setAvailableCopies(newAvailable);
        bookRepository.save(book);
        bookService.invalidateAvailabilityCache(book.getId());

        // Audit Log
        String auditDetail = String.format("Staff checkout barcode %s for %s (%s). Due: %s%s",
                copy.getBarcode(), member.getName(), member.getMemberId(), dueDate,
                request.isOverrideLimit() ? " [OVERRIDE: " + request.getOverrideReason() + "]" : "");
        auditLogService.record(staffEmail, "STAFF", "CHECKOUT", "LOAN", loanId, auditDetail);

        return BorrowResponse.builder()
                .loanId(loanId)
                .bookTitle(book.getTitle())
                .borrowedAt(now)
                .dueDate(dueDate)
                .copyId(copy.getId())
                .barcode(copy.getBarcode())
                .message("Circulation checkout successful! " + (request.isOverrideLimit() ? "(Staff Override Applied)" : ""))
                .build();
    }

    public ReturnResponse staffReturn(StaffReturnRequest request, String staffEmail) {
        BookCopy copy = copyRepository.findByBarcode(request.getBarcode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found with barcode: " + request.getBarcode()));

        Loan loan = loanRepository.findByCopyIdAndStatus(copy.getId(), LoanStatus.ACTIVE).orElse(null);
        Instant returnedAt = Instant.now();
        int overdueDays = 0;
        double fineAmount = 0.0;
        String fineId = null;

        if (loan != null) {
            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnedAt(returnedAt);

            // Calculate overdue if any
            if (returnedAt.isAfter(loan.getDueDate())) {
                overdueDays = (int) ChronoUnit.DAYS.between(loan.getDueDate(), returnedAt);
                fineAmount = overdueDays * 1.50;
                fineId = idGenerator.nextPrefixedId("FINE");

                Fine fine = Fine.builder()
                        .id(idGenerator.nextIdString())
                        .fineId(fineId)
                        .loanId(loan.getLoanId())
                        .memberId(loan.getMemberId())
                        .overdueDays(overdueDays)
                        .ratePerDay(1.50)
                        .amount(fineAmount)
                        .status(FineStatus.UNPAID)
                        .createdAt(returnedAt)
                        .build();
                fineRepository.save(fine);
                loan.setFineCalculated(true);
            }
            loanRepository.save(loan);
        }

        String condition = request.getCondition() != null ? request.getCondition().toUpperCase() : "NORMAL";
        if ("DAMAGED".equals(condition)) {
            copy.setStatus(CopyStatus.DAMAGED);
            copy.setCurrentHolderId(null);
            copy.setCurrentLoanId(null);
            if (request.getStaffNotes() != null) {
                copy.setConditionNotes("DAMAGED: " + request.getStaffNotes());
            }
            if (request.getDamageFee() != null && request.getDamageFee() > 0 && loan != null) {
                String dmgFineId = idGenerator.nextPrefixedId("FINE");
                Fine dmgFine = Fine.builder()
                        .id(idGenerator.nextIdString())
                        .fineId(dmgFineId)
                        .loanId(loan.getLoanId())
                        .memberId(loan.getMemberId())
                        .overdueDays(0)
                        .ratePerDay(0)
                        .amount(request.getDamageFee())
                        .status(FineStatus.UNPAID)
                        .notes("Damage assessment: " + request.getStaffNotes())
                        .createdAt(returnedAt)
                        .build();
                fineRepository.save(dmgFine);
            }
            copyRepository.save(copy);
        } else if ("LOST".equals(condition)) {
            copy.setStatus(CopyStatus.LOST);
            copy.setCurrentHolderId(null);
            copy.setCurrentLoanId(null);
            if (request.getStaffNotes() != null) {
                copy.setConditionNotes("LOST: " + request.getStaffNotes());
            }
            double fee = request.getDamageFee() != null && request.getDamageFee() > 0 ? request.getDamageFee() : 500.00;
            if (loan != null) {
                String lostFineId = idGenerator.nextPrefixedId("FINE");
                Fine lostFine = Fine.builder()
                        .id(idGenerator.nextIdString())
                        .fineId(lostFineId)
                        .loanId(loan.getLoanId())
                        .memberId(loan.getMemberId())
                        .overdueDays(0)
                        .ratePerDay(0)
                        .amount(fee)
                        .status(FineStatus.UNPAID)
                        .notes("Lost copy replacement fee: " + request.getStaffNotes())
                        .createdAt(returnedAt)
                        .build();
                fineRepository.save(lostFine);
            }
            copyRepository.save(copy);
        } else {
            // NORMAL return: check reservation queue
            Reservation pendingReservation = reservationRepository
                    .findFirstByBookIdAndStatusOrderByQueuePositionAsc(copy.getBookId(), ReservationStatus.PENDING)
                    .orElse(null);

            if (pendingReservation != null) {
                pendingReservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
                pendingReservation.setAllocatedCopyId(copy.getId());
                pendingReservation.setReadyAt(returnedAt);
                pendingReservation.setExpiresAt(returnedAt.plus(Duration.ofHours(48)));
                reservationRepository.save(pendingReservation);

                copy.setStatus(CopyStatus.RESERVED_FOR_PICKUP);
                copy.setCurrentHolderId(pendingReservation.getMemberId());
                copy.setCurrentLoanId(null);
                copy.setPickupDeadline(pendingReservation.getExpiresAt());
                copyRepository.save(copy);
            } else {
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setCurrentHolderId(null);
                copy.setCurrentLoanId(null);
                copy.setPickupDeadline(null);
                copyRepository.save(copy);
            }
        }

        // Invalidate book cache
        Book book = bookRepository.findById(copy.getBookId()).orElse(null);
        if (book != null) {
            int newAvailable = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
            book.setAvailableCopies(newAvailable);
            bookRepository.save(book);
            bookService.invalidateAvailabilityCache(book.getId());
        }

        auditLogService.record(staffEmail, "STAFF", "RETURN", "COPY", copy.getBarcode(),
                String.format("Returned barcode %s [Condition: %s, Fine: INR %.2f]. Staff Notes: %s",
                        copy.getBarcode(), condition, fineAmount, request.getStaffNotes()));

        return ReturnResponse.builder()
                .loanId(loan != null ? loan.getLoanId() : "N/A")
                .copyId(copy.getId())
                .status("RETURNED")
                .returnedAt(returnedAt)
                .overdueDays(overdueDays)
                .fineAmount(fineAmount)
                .fineId(fineId)
                .message("Return processed successfully. Copy condition: " + condition)
                .build();
    }

    public List<Loan> getAllLoans(LoanStatus status, String search) {
        List<Loan> list;
        if (status != null) {
            list = loanRepository.findByStatusOrderByBorrowedAtDesc(status);
        } else {
            list = loanRepository.findAllByOrderByBorrowedAtDesc();
        }

        Map<String, String> memberCodeMap = memberRepository.findAll().stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(
                        Member::getId,
                        m -> (m.getMemberId() != null && !m.getMemberId().isBlank())
                                ? m.getMemberId()
                                : ("MEM-" + m.getId().substring(Math.max(0, m.getId().length() - 4))),
                        (a, b) -> a));

        for (Loan l : list) {
            String code = null;
            if (l.getMemberCode() != null && l.getMemberCode().startsWith("MEM-")) {
                code = l.getMemberCode();
            } else if (l.getMemberId() != null && l.getMemberId().startsWith("MEM-")) {
                code = l.getMemberId();
            } else if (l.getMemberId() != null && memberCodeMap.containsKey(l.getMemberId())) {
                code = memberCodeMap.get(l.getMemberId());
            } else if (l.getMemberId() != null && !l.getMemberId().isBlank()) {
                code = "MEM-" + l.getMemberId().substring(Math.max(0, l.getMemberId().length() - 4));
            } else {
                code = "MEM-0001";
            }
            l.setMemberCode(code);
            l.setMemberId(code);
        }

        if (search != null && !search.isBlank()) {
            String s = search.trim().toLowerCase();
            list = list.stream()
                    .filter(l -> (l.getBookTitle() != null && l.getBookTitle().toLowerCase().contains(s))
                            || (l.getBarcode() != null && l.getBarcode().toLowerCase().contains(s))
                            || (l.getMemberId() != null && l.getMemberId().toLowerCase().contains(s))
                            || (l.getMemberCode() != null && l.getMemberCode().toLowerCase().contains(s)))
                    .toList();
        }
        return list;
    }
}
