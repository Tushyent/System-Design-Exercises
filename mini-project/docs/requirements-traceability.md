# Requirements Traceability Matrix

**Project**: Scalable Library Management System  
**Coursework**: SSN CSE V Semester UCS3513 System Design Laboratory  
**Document**: Requirements & Verification Traceability  

---

| Req ID | Requirement Description | Backend Implementation | API Endpoint | Frontend Workflow | Verification Test | Status | Evidence |
|---|---|---|---|---|---|---|---|
| **FR-01** | Member Registration | `AuthService.register()` | `POST /api/auth/register` | `AuthModal.tsx` Registration tab | `MemberServiceTest` | **PASS** | BCrypt password hash stored in MongoDB, returns JWT |
| **FR-02** | Member Authentication | `AuthService.login()` | `POST /api/auth/login` | `AuthModal.tsx` Login modal | Integration suite | **PASS** | Validates credentials, issues HMAC-SHA256 JWT |
| **FR-03** | Role-Based Access Control | `SecurityConfig`, `JwtAuthenticationFilter` | Filter intercepts all `/api/**` | Dynamic tab display based on `user.role` | Security filter tests | **PASS** | MEMBER allowed self-service; ADMIN/LIBRARIAN allowed catalogue mutation |
| **FR-04** | Book & Catalogue Search | `BookService.searchBooks()` | `GET /api/books/search` | `CataloguePage.tsx` search input & filters | API workflow tests | **PASS** | Multi-field search over title, author, category with pagination |
| **FR-05** | Book Details & Availability | `BookService.getBookDetails()` | `GET /api/books/{id}` | `BookDetailModal.tsx` | Unit & API tests | **PASS** | Returns title metadata + exact copy-level barcodes & statuses |
| **FR-06** | Atomic Book Borrowing | `CirculationService.borrowBook()` | `POST /api/circulation/borrow` | `BookDetailModal.tsx` "Borrow Book" button | `CirculationConcurrencyTest` | **PASS** | Atomic copy status transition (`AVAILABLE` -> `BORROWED`), 20-thread concurrency test verified |
| **FR-07** | Book Returning | `CirculationService.returnBook()` | `POST /api/circulation/return` | `MyLoansPage.tsx` "Return Copy" button | `CirculationConcurrencyTest` | **PASS** | Updates loan to `RETURNED`, auto-checks reservation queue |
| **FR-08** | Reservation Management | `ReservationService.createReservation()` | `POST /api/reservations` | `BookDetailModal.tsx` "Reserve Copy" button | Reservation queue tests | **PASS** | FIFO queue positioning, auto-allocation when copy returned |
| **FR-09** | Overdue Fine Calculation | `FineService.calculateOverdueFines()` | `GET /api/fines/my-fines` | `FinesPage.tsx` | `FineServiceTest` | **PASS** | Calculates daily fee for loans past due date |
| **FR-10** | Idempotent Fine Payments | `PaymentService.processPayment()` | `POST /api/payments/pay` | `FinesPage.tsx` payment modal | `IdempotentPaymentTest` | **PASS** | Client idempotency key guarantees exactly-once charge execution |
| **FR-11** | Administrative Book Addition | `BookService.createBook()` | `POST /api/books` | `AdminPage.tsx` Add Book form | Integration tests | **PASS** | Snowflake IDs generated for title and initial physical copies |
| **FR-12** | Physical Copy Batch Allocation | `BookService.addCopies()` | `POST /api/books/{id}/copies` | `AdminPage.tsx` Add Copies form | API workflow tests | **PASS** | Allocates new copies with unique deterministic barcodes |

---

## System Design Components Traceability

| Component ID | System Design Component | Architectural Justification | Backend Implementation | API Endpoint | Frontend Visualization | Automated Verification Test | Status |
|---|---|---|---|---|---|---|---|
| **SD-01** | **Rate Limiter** | Protects catalogue and authentication from DDoS & brute force | `TokenBucketRateLimiter`, `RateLimitInterceptor` | Intercepts all requests | Rejection stats on `SystemDesignDashboard.tsx` | `TokenBucketRateLimiterTest` | **PASS** |
| **SD-02** | **Database Sharding** | Partitions book & member datasets across independent storage nodes | `ShardingService`, `ShardStatsController` | `GET /api/sharding/stats` | Shard load distribution chart on `SystemDesignDashboard.tsx` | Sharding distribution analyzer | **PASS** |
| **SD-03** | **Consistent Hashing** | Distributes request keys across application server nodes with minimal churn | `ConsistentHashRouter`, `ConsistentHashSimulationService` | `GET /api/consistenthash/ring`, `POST /api/consistenthash/simulate` | Interactive ring visualizer & simulator on `SystemDesignDashboard.tsx` | `ConsistentHashRingTest` | **PASS** |
| **SD-04** | **Key-Value Store (Redis)** | High-speed cache-aside caching for popular book detail lookups | `BookService` cache-aside + `RedisConfig` | `GET /api/books/{id}` (uses cache) | Cache hit ratio on `SystemDesignDashboard.tsx` | Cache hit/miss latency benchmark | **PASS** |
| **SD-05** | **Unique ID Generator** | High-throughput distributed 64-bit ID generation without central database lock | `SnowflakeIdGenerator` | Used internally across all domain entities | Total IDs & collisions on `SystemDesignDashboard.tsx` | `SnowflakeIdGeneratorTest` (50,000 IDs, 20 threads, 0 collisions) | **PASS** |
| **SD-06** | **URL Shortener** | Compact shareable Base62 links for catalogue titles | `Base62Encoder`, `ShortUrlService` | `POST /api/urls/shorten`, `GET /s/{code}` | Shareable link copy button on `BookDetailModal.tsx` | `Base62EncoderTest` | **PASS** |
| **SD-07** | **Web Crawler** | Multi-threaded ingestion of book metadata from external libraries | `WebCrawlerService`, `MockCatalogExternalController` | `POST /api/crawler/trigger`, `GET /api/crawler/jobs` | Crawler orchestration & job history table on `AdminPage.tsx` | Crawler deduplication & ingestion test | **PASS** |
| **SD-08** | **Autocomplete** | Sub-millisecond prefix suggestion tree for titles, authors, categories | `PrefixTrie`, `AutocompleteService` | `GET /api/autocomplete`, `GET /api/autocomplete/evaluate` | Interactive prefix dropdown on `AutocompleteInput.tsx` | `PrefixTrieTest` & Precision@k benchmark | **PASS** |
