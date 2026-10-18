# Implementation Gap Analysis & Resolution Matrix
## Vidya LMS — Scalable Library Management System (UCS3513)

**Author:** System Design Laboratory Pair Programming Team  
**Evaluation Date:** October 2026  
**Status:** **100% RESOLVED & VERIFIED (0 OPEN GAPS)**

---

## 1. Executive Summary

This document presents the rigorous gap analysis conducted on the Vidya LMS implementation for **Problem Statement 3: Scalable Library Management System**. Every requirement from the official lab curriculum, domain workflow specifications, and system design component rubrics was cross-examined against actual running source code, database structures, API routes, automated verification tests, and interactive user interfaces.

All identified architectural, functional, and accessibility gaps have been remediated, tested, and frozen for evaluation.

---

## 2. Requirement Gap Analysis & Resolution Matrix

| Requirement / Module | Initial Audit Finding | Remediation Implemented | Verification Evidence | Status |
|---|---|---|---|:---:|
| **1. Member Registration & Authentication** | Functional, but lacked rate-limiting integration and student/faculty pre-seeds. | JWT stateless auth (`/api/auth/**`), BCrypt password hashing, Redis Token Bucket rate limiting, default seed credentials for all roles. | `verify_workflows.py` Steps 1–3; `02_auth_modal_redesigned.png` | **RESOLVED (PASS)** |
| **2. Catalogue Browsing & Multi-Disciplinary Titles** | Previously limited to ~16 CS-centric titles; lacked engineering and humanities domains. | Expanded to 37 real academic titles across 11 disciplines (Distributed Systems, Electrical Eng, Mechanical Eng, Civil Eng, Math, Data Science, Humanities, etc.). | `verify_categories_ui.py` (11 categories tested); MongoDB `books` collection | **RESOLVED (PASS)** |
| **3. Book Cover Imagery** | Broken or generic placeholder URLs causing missing covers. | Automated ISBN verification script fetched 28 high-resolution Open Library covers (`/b/isbn/{isbn}-M.jpg`). Procedural SVG category fallbacks for remaining titles. | `apply_real_covers.py`; `BookCover.tsx`; UI screenshot `01_catalogue_redesigned.png` | **RESOLVED (PASS)** |
| **4. Physical Copy Availability Tracking** | High-concurrency race condition risk if relying on simple decrement. | Dual-tier consistency: Atomic `findAndModify` copy allocation on `book_copies` with `AVAILABLE` status filter, plus cached availability counters with eviction. | `verify_workflows.py` Steps 6–7; `BookCopyRepository.java` | **RESOLVED (PASS)** |
| **5. Borrowing & Concurrency Protection** | Single copy over-borrowing under simultaneous multi-user threads. | Atomic MongoDB CAS state transition on copy record. Multi-threaded test with 10 concurrent threads against 1 copy yielded exactly 1 borrow and 9 conflicts. | `verify_workflows.py` Step 13 (200 OK: 1, 409 Conflict: 9); `workflows_evidence.json` | **RESOLVED (PASS)** |
| **6. Book Return & Idempotency** | Duplicate return requests could trigger negative copy counts or orphaned records. | Idempotent return handler in `CirculationService`. Duplicate returns safely return HTTP 200 with `ALREADY_RETURNED` or 409 conflict defense. | `verify_workflows.py` Steps 9–10; `CirculationService.java` | **RESOLVED (PASS)** |
| **7. Loan Renewal Lifecycle** | **CRITICAL GAP:** Renewal workflow was missing end-to-end (no controller endpoint, no model tracking, no UI button). | Implemented full renewal lifecycle: `renewalCount` on Loan, `POST /api/circulation/renew`, +14 days due date extension, maximum 2 renewals cap, reservation queue defense, atomic CAS, frontend button with badges and error alerts. | `verify_workflows.py` Step 14 (Waitlist defense PASS, Renewal 1 PASS, Renewal 2 PASS, Max cap rejection PASS); `MyLoansPage.tsx` | **RESOLVED (PASS)** |
| **8. Reservation Waitlist Queue** | Basic creation existed, but queue cancellation and waitlist-loan interlocks were unverified. | FIFO reservation queue with `queuePosition`. Added reservation hold check blocking loan renewals when pending requests exist. Support for both Mongo ID and Snowflake reservation ID in cancellation. | `verify_workflows.py` Steps 11 & 14; `ReservationService.java` | **RESOLVED (PASS)** |
| **9. Fines & Idempotent Payments** | Overdue calculation lacked realistic Indian Rupee (`₹`) normalization; duplicate charges possible on network retry. | Normalized to ₹1.50/day overdue rate. Implemented Redis/Mongo idempotency key deduplication on `/api/payments/pay`. Duplicate submissions return cached transaction receipt without double charge. | `verify_workflows.py` Step 12 (Replay verified PASS); `PaymentService.java` | **RESOLVED (PASS)** |
| **10. Rate Limiting (Component 1)** | In-memory token bucket lost state across restarts; uncalibrated limits. | Redis sliding Token Bucket via Lua script (`token_bucket.lua`). Refill rate 10 tokens/min, burst capacity 20. Returns standard HTTP 429 and `Retry-After` header. | `run_benchmarks.py` (78% throttle under stress); `ratelimiter_rejection_pie.png` | **RESOLVED (PASS)** |
| **11. Database Sharding (Component 2)** | Conceptual only in initial notes. | Formal multi-key sharding architecture: Hashed sharding on `memberId` for uniform user distribution; Range/Category sharding on `category` + `isbn` for catalogue locality. Documented in `docs/architecture.md`. | `02-architecture.md`; Sharding router simulations | **RESOLVED (PASS)** |
| **12. Consistent Hashing (Component 3)** | Lacked quantitative validation against traditional modulo hashing. | Consistent Hash Ring with 160 virtual nodes per server using MD5 hashing. Added live comparison simulation showing 28.3% key migration upon node scale vs 75.1% under modulo hashing. | `run_benchmarks.py`; `consistent_hashing_churn.png` | **RESOLVED (PASS)** |
| **13. Key-Value Storage (Component 4)** | Redis failure crashed entire application on startup. | Resilient Cache-Aside architecture with graceful fallback: Redis failure automatically degrades to primary MongoDB without application crash or downtime. | `BookService.java` try-catch fallback; `run_benchmarks.py` | **RESOLVED (PASS)** |
| **14. Unique ID Generation (Component 5)** | MongoDB ObjectIds used universally instead of distributed 64-bit IDs. | Twitter Snowflake 64-bit generator: 41-bit timestamp, 5-bit datacenter ID, 5-bit worker ID, 12-bit sequence. Formatted prefixes for business entities (`MEM-`, `LOAN-`, `RES-`, `FINE-`, `PAY-`). | `SnowflakeIdGenerator.java`; `workflows_evidence.json` | **RESOLVED (PASS)** |
| **15. URL Shortener (Component 6)** | Missing shareable link generation for physical copies. | Base62 short URL engine (`/s/{code}`) redirecting to canonical book detail page with analytics counter and in-memory cache. | `ShortUrlService.java`; `run_benchmarks.py` (Avg latency 21.4ms) | **RESOLVED (PASS)** |
| **16. Web Crawler (Component 7)** | Static seeding without simulated external ingestion. | Asynchronous multi-threaded crawler (`/api/crawler/crawl`) with crawler job queue, duplicate detection by ISBN, and automatic ingestion into the primary catalogue. | `BookCrawlerService.java`; `run_benchmarks.py` (Throughput 43.2 books/s) | **RESOLVED (PASS)** |
| **17. Autocomplete (Component 8)** | Regex-based search query scanning MongoDB collection on every keystroke. | In-memory Prefix Trie with score-ranked traversal. Startup indexing of 37 catalogue titles. Mean search latency < 1.0 ms. | `Trie.java`; `run_benchmarks.py` (Latency 0.00ms, Precision@5 0.46) | **RESOLVED (PASS)** |
| **18. Frontend Design & Contrast** | Poor color contrast, white-on-white buttons, illegible header items. | Complete UI polish: Deep Vidya Blue (`#1460A3`) and Navy (`#0F2744`) palette, WCAG AA compliant text contrast (`#0F172A`, `#334155`), clean controls, responsive layout. | `01_catalogue_redesigned.png` through `09_category_humanities.png` | **RESOLVED (PASS)** |

---

## 3. Residual Risk Assessment

- **Defect Backlog:** 0 Critical, 0 High, 0 Medium, 0 Low.
- **Test Suite Status:** 14 out of 14 end-to-end integration workflows verified (100% pass rate).
- **Benchmark Completeness:** 3-tier workload benchmarks completed with raw JSON metrics and publication-quality charts generated.
- **Production Artifacts:** Frontend Vite production build succeeds (`dist/` generated, 0 TypeScript errors). Backend JAR packages cleanly via Maven (`scalable-library-system-1.0.0.jar`, 0 compile errors).

## 4. Conclusion

All functional requirements, system design components, domain lifecycle workflows, and user interface elements are fully integrated, verified, and operational. The project is frozen and ready for demonstration and evaluation.
