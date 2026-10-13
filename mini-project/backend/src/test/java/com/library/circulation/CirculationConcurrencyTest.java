package com.library.circulation;

import com.library.catalogue.dto.CreateBookRequest;
import com.library.catalogue.model.Book;
import com.library.catalogue.model.BookCopy;
import com.library.catalogue.model.CopyStatus;
import com.library.catalogue.repository.BookCopyRepository;
import com.library.catalogue.repository.BookRepository;
import com.library.catalogue.service.BookService;
import com.library.circulation.dto.BorrowRequest;
import com.library.circulation.model.Loan;
import com.library.circulation.model.LoanStatus;
import com.library.circulation.repository.LoanRepository;
import com.library.circulation.service.CirculationService;
import com.library.common.exception.ConflictException;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.model.Member;
import com.library.members.model.Role;
import com.library.members.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CirculationConcurrencyTest {

    @Autowired
    private CirculationService circulationService;

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Test
    @DisplayName("Concurrency Test: 20 concurrent members attempting to borrow 1 single copy -> exactly 1 succeeds, 19 receive conflict")
    void testConcurrentBorrowContention() throws InterruptedException {
        // 1. Create a book with strictly 1 single physical copy
        String bookTitle = "Concurrency Test Book " + System.currentTimeMillis();
        CreateBookRequest bookReq = CreateBookRequest.builder()
                .title(bookTitle)
                .isbn("TEST-ISBN-" + System.currentTimeMillis())
                .authors(List.of("Concurrency Author"))
                .categories(List.of("Testing"))
                .initialCopies(1)
                .build();
        Book book = bookService.createBook(bookReq);

        // Verify strictly 1 copy exists
        List<BookCopy> copies = copyRepository.findByBookId(book.getId());
        assertEquals(1, copies.size());
        assertEquals(CopyStatus.AVAILABLE, copies.get(0).getStatus());

        // 2. Create 20 unique test members
        int concurrencyLevel = 20;
        List<Member> testMembers = new ArrayList<>();
        for (int i = 0; i < concurrencyLevel; i++) {
            Member m = Member.builder()
                    .id(idGenerator.nextIdString())
                    .memberId("TEST-MEM-" + idGenerator.nextIdString().substring(8))
                    .name("Test User " + i)
                    .email("testuser" + i + "_" + System.currentTimeMillis() + "@ssn.edu.in")
                    .passwordHash("hashed")
                    .role(Role.MEMBER)
                    .status("ACTIVE")
                    .maxBorrowLimit(5)
                    .build();
            testMembers.add(memberRepository.save(m));
        }

        // 3. Launch 20 concurrent borrow requests
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrencyLevel);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (Member member : testMembers) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads at the start line
                    BorrowRequest borrowReq = BorrowRequest.builder()
                            .bookId(book.getId())
                            .build();

                    circulationService.borrowBook(borrowReq, member.getId());
                    successCount.incrementAndGet();
                } catch (ConflictException ce) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // 4. Assert Concurrency Safety Guarantees
        assertEquals(1, successCount.get(), "EXACTLY ONE concurrent thread must successfully borrow the single copy");
        assertEquals(concurrencyLevel - 1, conflictCount.get(), "All other concurrent threads must receive 409 Conflict");

        // Verify Database State Integrity
        BookCopy copyAfter = copyRepository.findById(copies.get(0).getId()).orElseThrow();
        assertEquals(CopyStatus.BORROWED, copyAfter.getStatus());
        assertNotNull(copyAfter.getCurrentLoanId());

        long activeLoansForCopy = loanRepository.findAll().stream()
                .filter(l -> l.getCopyId().equals(copyAfter.getId()) && l.getStatus() == LoanStatus.ACTIVE)
                .count();
        assertEquals(1, activeLoansForCopy, "There must be exactly ONE active loan for the copy");
    }
}
