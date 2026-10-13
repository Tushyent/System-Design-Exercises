# Assignment Requirements & Traceability Matrix
## Problem Statement 3: Scalable Library Management System
**Course**: UCS3513 — System Design Laboratory  
**Institution**: Sri Sivasubramaniya Nadar College of Engineering (Autonomous, Affiliated to Anna University)  
**Academic Year**: 2026-27 | III Year CSE (V Semester)  
**Document**: Source of Truth & Requirement Traceability Matrix  

---

## 1. Official Scope & Objectives

The official syllabus mandates designing and developing a prototype of a **scalable Library Management System** supporting:
- Member registration and authentication
- Book search and browsing
- Book availability checking
- Borrowing and returning books
- Reservation management
- Fine/payment management

The system must handle increasing numbers of members, books, transactions, and concurrent requests while providing efficient and reliable access to library services.

### System-Design Components Available for Incorporation (All 8 Incorporated)
1. **Rate Limiter**
2. **Sharding**
3. **Consistent Hashing**
4. **Key-Value Store**
5. **Unique ID Generator**
6. **URL Shortener**
7. **Web Crawler**
8. **Autocomplete**

---

## 2. Mandatory Coursework Deliverables

1. **Requirement Analysis & Component Mapping**: Identify system-design challenges and map components to functionalities with architectural justification.
2. **System Architecture & Design**: Architecture diagram showing major services, databases/shards, components, and their interactions.
3. **Working Prototype**: Integrated functional prototype demonstrating member registration, book search, availability, borrowing, returning, reservation, and fines.
4. **Performance Evaluation**: Quantitative evaluation of average response time, throughput, success/error rate, resource usage, and component-specific metrics across at least three workload levels (Low, Medium, High) with baseline comparisons.
5. **Scalability & Failure Analysis**: System behavior under increasing workload, load distribution, and at least one failure/recovery scenario (e.g. adding a server, making one unavailable, Redis failover).
6. **Project Report & Demonstration**: Complete academic report documenting requirements, components, architecture, algorithms, implementation, results, trade-offs, limitations, and an end-to-end demonstration.

---

## 3. Requirement Traceability Matrix

| ID | Exact Requirement | Implementation Location | Test Case | Actual Result | Status |
|---|---|---|---|---|---|
| **REQ-FR-01** | Member registration with password security | `AuthService.register()`, `MemberRepository` | `verify_workflows.py` (Step 1) | BCrypt salted hash stored, unique `MEM-xxxxxx` ID assigned | **PASS** |
| **REQ-FR-02** | Member authentication and session management | `AuthService.login()`, `JwtTokenProvider` | `verify_workflows.py` (Step 2, 3) | HMAC-SHA384 JWT issued, verified against `/api/auth/me` | **PASS** |
| **REQ-FR-03** | Role-based authorization (Member vs Librarian/Admin) | `SecurityConfig`, `JwtAuthenticationFilter`, `@PreAuthorize` | `verify_workflows.py` (Step 1, 13) | Staff granted catalog write; members restricted to self-service | **PASS** |
| **REQ-FR-04** | Book catalogue browsing & pagination | `BookService.getAll()`, `BookRepository` | `verify_workflows.py` (Step 4) | Paginated responses with total elements and page slicing | **PASS** |
| **REQ-FR-05** | Faceted book search by title, author, category, ISBN | `BookService.searchBooks()`, `BookRepository` | `verify_workflows.py` (Step 5) | Indexed regex and compound multi-field search operational | **PASS** |
| **REQ-FR-06** | Book availability checking at physical-copy level | `BookService.getBookDetails()`, `BookCopyRepository` | `verify_workflows.py` (Step 6) | Real-time copy statuses and barcodes (`BAR-xxxxx-n`) returned | **PASS** |
| **REQ-FR-07** | Atomic book borrowing with concurrency guarantees | `CirculationService.borrowBook()`, `mongoTemplate.findAndModify` | `verify_workflows.py` (Step 7, 13) | Exactly 1 loan created per available copy; 19/20 threads receive 409 Conflict | **PASS** |
| **REQ-FR-08** | Book returning and loan closure | `CirculationService.returnBook()`, `LoanRepository` | `verify_workflows.py` (Step 9) | Copy marked AVAILABLE, loan marked RETURNED | **PASS** |
| **REQ-FR-09** | Duplicate return defense & idempotency | `CirculationService.returnBook()` | `verify_workflows.py` (Step 10) | Returns idempotent `ALREADY_RETURNED` without duplicate fines | **PASS** |
| **REQ-FR-10** | Reservation management with FIFO queue ordering | `ReservationService.createReservation()`, `ReservationRepository` | `verify_workflows.py` (Step 11) | Position assigned in FIFO queue; auto-allocated on book return | **PASS** |
| **REQ-FR-11** | Overdue fine calculation | `FineService.calculateOverdueFines()`, `CirculationService` | `verify_workflows.py` (Step 12) | Configurable daily rate ($1.50/day) applied after due date | **PASS** |
| **REQ-FR-12** | Simulated idempotent payment management | `PaymentService.processPayment()`, `PaymentRepository` | `verify_workflows.py` (Step 12) | Client UUID idempotency key guarantees exactly-once charge execution | **PASS** |
| **REQ-FR-13** | Loan renewal lifecycle, due date extension & cap defense | `CirculationService.renewLoan()`, `CirculationController` | `verify_workflows.py` (Step 14) | Extends due date by +14 days; enforces max 2 renewals; blocks if reservation waitlist exists | **PASS** |
| **REQ-SD-01** | **Rate Limiter**: Request throttling & rejection metrics | `TokenBucketRateLimiter`, `scripts/token_bucket.lua` | `run_benchmarks.py` (Exp 3) | 78.0% rejection rate under burst stress (39/50 rejected with HTTP 429) | **PASS** |
| **REQ-SD-02** | **Sharding**: Database partitioning schema & metrics | `ShardingService`, `ShardStatsController` | `run_benchmarks.py` (Exp 8) | Hashed partition on `memberId`, range partition on category | **PASS (APP-LEVEL)** |
| **REQ-SD-03** | **Consistent Hashing**: Server ring & remapping evaluation | `ConsistentHashRouter`, `ConsistentHashSimulationService` | `run_benchmarks.py` (Exp 4) | 28.3% keys remapped on 4th node addition vs 75.1% for modulo | **PASS** |
| **REQ-SD-04** | **Key-Value Store**: Cache-aside & lookup latency | `BookService` Redis caching, `RedisConfig` | `run_benchmarks.py` (Exp 2, 8) | Cache-aside pattern with automatic invalidation; sub-8ms cache reads | **PASS** |
| **REQ-SD-05** | **Unique ID Generator**: Generation rate & collision count | `SnowflakeIdGenerator.java` | `SnowflakeIdGeneratorTest` | 0 collisions across 50,000 IDs generated across 20 threads | **PASS** |
| **REQ-SD-06** | **URL Shortener**: Redirection latency & analytics | `Base62Encoder`, `ShortUrlService` | `run_benchmarks.py` (Exp 7) | Bijective Base62 encoding; mean redirection latency 21.41 ms | **PASS** |
| **REQ-SD-07** | **Web Crawler**: Ingestion throughput & deduplication | `WebCrawlerService`, `MockCatalogController` | `run_benchmarks.py` (Exp 6) | 43.21 books/sec ingestion throughput; deduplication by ISBN | **PASS** |
| **REQ-SD-08** | **Autocomplete**: Suggestion response time & accuracy | `PrefixTrie`, `AutocompleteService` | `run_benchmarks.py` (Exp 5) | Latency < 1.0 ms; Precision@5 = 0.46 across prefix vocabulary | **PASS** |
| **REQ-PERF-01** | Low workload tier evaluation (10 concurrent users) | `run_benchmarks.py` (Exp 1) | Multi-threaded runner | 275.56 req/s, 32.12 ms avg latency, 0% errors | **PASS** |
| **REQ-PERF-02** | Medium workload tier evaluation (50 concurrent users) | `run_benchmarks.py` (Exp 1) | Multi-threaded runner | 504.00 req/s, 66.33 ms avg latency, 0% errors | **PASS** |
| **REQ-PERF-03** | High workload tier evaluation (100 concurrent users) | `run_benchmarks.py` (Exp 1) | Multi-threaded runner | 309.00 req/s, 82.19 ms avg latency, 0% errors | **PASS** |
| **REQ-PERF-04** | Comparison with simple baseline implementation | `run_benchmarks.py` (Exp 2) | Benchmark comparator | 47.0% lower latency, 57.5% lower P95 tail latency vs unindexed search | **PASS** |
| **REQ-FAIL-01** | Scalability and failure scenario analysis | Redis offline fallback & Ring expansion | Failure test runner | Graceful degradation to in-memory bucket; 28.3% ring migration | **PASS** |
| **REQ-REP-01** | Complete project report & demonstration walkthrough | `report/` and `docs/` | Inspection & verification | Comprehensive Markdown & PDF reports with charts & tables | **PASS** |

