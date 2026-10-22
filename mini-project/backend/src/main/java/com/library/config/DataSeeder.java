package com.library.config;

import com.library.catalogue.dto.CreateBookRequest;
import com.library.catalogue.model.Book;
import com.library.catalogue.model.BookCopy;
import com.library.catalogue.model.CopyStatus;
import com.library.catalogue.repository.BookCopyRepository;
import com.library.catalogue.repository.BookRepository;
import com.library.catalogue.service.BookService;
import com.library.circulation.model.Loan;
import com.library.circulation.model.LoanStatus;
import com.library.circulation.repository.LoanRepository;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.model.Member;
import com.library.members.model.Role;
import com.library.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final BookService bookService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public void run(String... args) {
        seedUsers();
        seedCatalogue();
        seedCirculationAndFines();
    }

    private void seedUsers() {
        if (memberRepository.count() > 0) {
            log.info("Users already exist in database, skipping user seeding");
            return;
        }

        log.info("Seeding initial users and administrative accounts...");

        // Admin Account
        Member admin = Member.builder()
                .id(idGenerator.nextIdString())
                .memberId("MEM-0001")
                .name("System Administrator")
                .email("admin@ssn.edu.in")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .status("ACTIVE")
                .maxBorrowLimit(50)
                .build();
        memberRepository.save(admin);

        // Librarian Account
        Member librarian = Member.builder()
                .id(idGenerator.nextIdString())
                .memberId("MEM-0002")
                .name("Chief Librarian")
                .email("librarian@ssn.edu.in")
                .passwordHash(passwordEncoder.encode("librarian123"))
                .role(Role.LIBRARIAN)
                .status("ACTIVE")
                .maxBorrowLimit(30)
                .build();
        memberRepository.save(librarian);

        // Test Member 1
        Member member1 = Member.builder()
                .id(idGenerator.nextIdString())
                .memberId("MEM-1001")
                .name("Alex Johnson")
                .email("member@ssn.edu.in")
                .passwordHash(passwordEncoder.encode("member123"))
                .role(Role.MEMBER)
                .status("ACTIVE")
                .maxBorrowLimit(5)
                .build();
        memberRepository.save(member1);

        // Test Member 2
        Member member2 = Member.builder()
                .id(idGenerator.nextIdString())
                .memberId("MEM-1002")
                .name("Tushyent N P")
                .email("tushyent@ssn.edu.in")
                .passwordHash(passwordEncoder.encode("member123"))
                .role(Role.MEMBER)
                .status("ACTIVE")
                .maxBorrowLimit(5)
                .build();
        memberRepository.save(member2);

        log.info("Seeded 4 users (admin, librarian, 2 members) successfully");
    }

    private void seedCatalogue() {
        if (bookRepository.count() > 0) {
            log.info("Books already exist in catalogue, re-indexing autocomplete trie...");
            for (Book b : bookRepository.findAll()) {
                bookService.indexInAutocomplete(b);
            }
            return;
        }

        log.info("Seeding rich catalogue dataset and initializing autocomplete prefix trie...");

        List<CreateBookRequest> initialBooks = List.of(
                CreateBookRequest.builder()
                        .title("Designing Data-Intensive Applications")
                        .isbn("9781449373320")
                        .authors(List.of("Martin Kleppmann"))
                        .categories(List.of("Distributed Systems", "Database Design", "System Design"))
                        .publisher("O'Reilly Media")
                        .publicationYear(2017)
                        .description("The definitive guide to architecture of data systems, replication, sharding, transactions, and stream processing.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51ZSpMl1-LL._SX379_BO1,204,203,200_.jpg")
                        .initialCopies(3)
                        .build(),

                CreateBookRequest.builder()
                        .title("Clean Code: A Handbook of Agile Software Craftsmanship")
                        .isbn("9780132350884")
                        .authors(List.of("Robert C. Martin"))
                        .categories(List.of("Software Engineering", "Programming"))
                        .publisher("Prentice Hall")
                        .publicationYear(2008)
                        .description("Even bad code can function. But if code isn't clean, it can bring a development organization to its knees.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/41xShlnTZTL._SX376_BO1,204,203,200_.jpg")
                        .initialCopies(3)
                        .build(),

                CreateBookRequest.builder()
                        .title("Introduction to Algorithms (CLRS)")
                        .isbn("9780262033848")
                        .authors(List.of("Thomas H. Cormen", "Charles E. Leiserson", "Ronald L. Rivest", "Clifford Stein"))
                        .categories(List.of("Computer Science", "Algorithms", "Data Structures"))
                        .publisher("MIT Press")
                        .publicationYear(2009)
                        .description("Comprehensive textbook covering all foundational and advanced algorithms and data structures.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/41SNoh5ZhOL._SX379_BO1,204,203,200_.jpg")
                        .initialCopies(4)
                        .build(),

                CreateBookRequest.builder()
                        .title("Operating System Concepts")
                        .isbn("9781119800361")
                        .authors(List.of("Abraham Silberschatz", "Peter B. Galvin", "Greg Gagne"))
                        .categories(List.of("Computer Science", "Operating Systems"))
                        .publisher("Wiley")
                        .publicationYear(2021)
                        .description("The dinosaur book: foundational coverage of processes, concurrency, memory management, and file systems.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51T-k96i-aL._SX397_BO1,204,203,200_.jpg")
                        .initialCopies(2)
                        .build(),

                CreateBookRequest.builder()
                        .title("Database System Concepts")
                        .isbn("9780078022159")
                        .authors(List.of("Abraham Silberschatz", "Henry F. Korth", "S. Sudarshan"))
                        .categories(List.of("Database Design", "Computer Science"))
                        .publisher("McGraw-Hill")
                        .publicationYear(2019)
                        .description("Core concepts of relational and non-relational database management, indexing, query optimization, and transactions.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51n2gYvE68L._SX397_BO1,204,203,200_.jpg")
                        .initialCopies(3)
                        .build(),

                CreateBookRequest.builder()
                        .title("Computer Networks")
                        .isbn("9780133827187")
                        .authors(List.of("Andrew S. Tanenbaum", "David J. Wetherall"))
                        .categories(List.of("Computer Science", "Networking"))
                        .publisher("Pearson")
                        .publicationYear(2013)
                        .description("Classic overview of network protocol layers from physical to application layer.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51wA1dJbQ4L._SX396_BO1,204,203,200_.jpg")
                        .initialCopies(2)
                        .build(),

                CreateBookRequest.builder()
                        .title("Clean Architecture: A Craftsman's Guide to Software Structure and Design")
                        .isbn("9780134494166")
                        .authors(List.of("Robert C. Martin"))
                        .categories(List.of("Software Engineering", "System Design"))
                        .publisher("Prentice Hall")
                        .publicationYear(2017)
                        .description("By applying universal rules of software architecture, you can dramatically improve developer productivity.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/41-sN-mzwKL._SX381_BO1,204,203,200_.jpg")
                        .initialCopies(2)
                        .build(),

                CreateBookRequest.builder()
                        .title("Building Microservices: Designing Fine-Grained Systems")
                        .isbn("9781491950357")
                        .authors(List.of("Sam Newman"))
                        .categories(List.of("Distributed Systems", "Cloud Computing", "System Design"))
                        .publisher("O'Reilly Media")
                        .publicationYear(2021)
                        .description("A holistic guide for distributed architectures, deployment models, security, and monitoring.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51r-7cEa+vL._SX379_BO1,204,203,200_.jpg")
                        .initialCopies(2)
                        .build(),

                CreateBookRequest.builder()
                        .title("Distributed Systems: Principles and Paradigms")
                        .isbn("9781530281756")
                        .authors(List.of("Andrew S. Tanenbaum", "Maarten van Steen"))
                        .categories(List.of("Distributed Systems", "Computer Science"))
                        .publisher("CreateSpace")
                        .publicationYear(2016)
                        .description("Covers communication, processes, naming, synchronization, consistency and replication in distributed environments.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51V668e1kGL._SX384_BO1,204,203,200_.jpg")
                        .initialCopies(2)
                        .build()
        );

        for (CreateBookRequest req : initialBooks) {
            Book saved = bookService.createBook(req);
            saved.setExternalRating(4.8);
            saved.setExternalSource("InternalCatalogue");
            bookRepository.save(saved);
        }

        log.info("Seeded {} book titles with individual copies and short URLs", initialBooks.size());
    }

    private void seedCirculationAndFines() {
        if (loanRepository.count() > 0) {
            return;
        }

        Member member = memberRepository.findByEmail("member@ssn.edu.in").orElse(null);
        Book book = bookRepository.findByIsbn("9781449373320").orElse(null);

        if (member != null && book != null) {
            List<BookCopy> copies = copyRepository.findByBookId(book.getId());
            if (!copies.isEmpty()) {
                BookCopy copyToLoan = copies.get(0);
                copyToLoan.setStatus(CopyStatus.BORROWED);
                copyToLoan.setCurrentHolderId(member.getId());

                String loanId = idGenerator.nextPrefixedId("LOAN");
                copyToLoan.setCurrentLoanId(loanId);
                copyRepository.save(copyToLoan);

                // Seed overdue loan (borrowed 20 days ago, due 6 days ago -> 6 days overdue)
                Instant now = Instant.now();
                Instant borrowedAt = now.minus(Duration.ofDays(20));
                Instant dueDate = now.minus(Duration.ofDays(6));

                Loan overdueLoan = Loan.builder()
                        .id(idGenerator.nextIdString())
                        .loanId(loanId)
                        .memberId(member.getId())
                        .memberCode(member.getMemberId())
                        .bookId(book.getId())
                        .bookTitle(book.getTitle())
                        .copyId(copyToLoan.getId())
                        .barcode(copyToLoan.getBarcode())
                        .borrowedAt(borrowedAt)
                        .dueDate(dueDate)
                        .status(LoanStatus.ACTIVE)
                        .fineCalculated(false)
                        .build();
                loanRepository.save(overdueLoan);

                // Pre-seed an outstanding fine of INR 9.00 (6 days * INR 1.50)
                String fineId = idGenerator.nextPrefixedId("FINE");
                Fine fine = Fine.builder()
                        .id(idGenerator.nextIdString())
                        .fineId(fineId)
                        .loanId(loanId)
                        .memberId(member.getId())
                        .overdueDays(6)
                        .ratePerDay(1.50)
                        .amount(9.00)
                        .status(FineStatus.UNPAID)
                        .createdAt(now.minus(Duration.ofDays(1)))
                        .build();
                fineRepository.save(fine);

                book.setAvailableCopies(copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE));
                bookRepository.save(book);

                log.info("Seeded initial active loan and overdue fine (INR 9.00) for member {}", member.getEmail());
            }
        }
    }
}
