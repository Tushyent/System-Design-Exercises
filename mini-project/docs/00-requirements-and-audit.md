# Phase 0: Requirements Extraction and Workspace Audit
## Scalable Library Management System (Problem Statement 3 - UCS3513)

### Executive Overview
This document formalizes the requirement extraction, non-functional targets, acceptance traceability, and workspace audit for the **Scalable Library Management System** (Question 3 of SSN CSE UCS3513 System Design Laboratory Mini-Project).

---

## 0.1 Functional Requirements (FR)

| Req ID | Requirement | Description | System Design Mapping |
|--------|-------------|-------------|-----------------------|
| **FR-01** | Member Registration | Register member with name, email, hashed password, role (MEMBER, LIBRARIAN, ADMIN), account status. | Snowflake ID Gen, Rate Limiter |
| **FR-02** | Member Authentication | Secure JWT-based authentication, token issue, refresh/validation, stateless session handling. | Token Bucket Rate Limiter (Brute-force protection) |
| **FR-03** | Role-Based Access Control (RBAC) | Distinct authorizations: Members (borrow, return, reserve, pay fines, share URLs); Librarians/Admins (catalogue management, physical copy management, crawl triggers, metrics monitoring). | Spring Security Filter Chain |
| **FR-04** | Browse Catalogue | Paginated list of catalogue titles with filtering and availability overview. | Redis Read-Through Cache |
| **FR-05** | Book Search | Multi-attribute search across title, author, category, ISBN with baseline vs indexed/cached performance modes. | MongoDB Indexes + Redis Cache |
| **FR-06** | Book Details & Enrichment | Rich view of book metadata, physical copy states, and external metadata enriched via web crawling. | Web Crawler, Redis Key-Value Store |
| **FR-07** | Copy-Level Availability Checking | Real-time copy status (AVAILABLE, BORROWED, RESERVED, MAINTENANCE) tracking individual physical barcodes. | Authoritative MongoDB conditional reads + Redis cache with immediate invalidation |
| **FR-08** | Concurrency-Safe Borrowing | Atomic copy claim preventing double-borrowing under concurrent requests. Verify borrowing limit and zero blocking fines. | MongoDB Atomic FindAndModify / Optimistic Locking + Distributed Lock |
| **FR-09** | Idempotent Book Return | Return borrowed copy, mark copy available or route to pending reservation queue, calculate overdue fines. | Idempotent Workflow + Snowflake ID Gen |
| **FR-10** | Overdue Fine Calculation | Calculate daily overdue penalties ($1.50/day overdue, capped at maximum $50.00). | Common Business Logic Engine |
| **FR-11** | Fine Balance & Payment History | View unpaid fines, accrued dates, and itemized payment receipts. | Sharded Transaction History |
| **FR-12** | Simulated Idempotent Payment | Process simulated fine payments safely using client idempotency keys to prevent duplicate billing. | Key-Value Store Idempotency Token Set |
| **FR-13** | Book Reservation | Reserve currently unavailable books when all copies are checked out. Prevent duplicate active reservations for same book by same member. | Reservation Queue Manager |
| **FR-14** | FIFO Reservation Queue | Priority queue for reservations. First member in queue is assigned the copy upon return. | Sharded Queue + Atomic Pointer Update |
| **FR-15** | Reservation Claim & Notification | Mark reservation ELIGIBLE_FOR_PICKUP with 48-hour pickup window. | State Machine |
| **FR-16** | Autocomplete Suggestions | Low-latency prefix search for book titles, authors, and categories with top-k ranking and relevance. | In-Memory Trie Data Structure + Redis Prefix Cache |
| **FR-17** | Shareable URL Shortener | Generate Base62 short links (`/s/{code}`) for books and reservation receipts with fast lookup and redirection. | Base62 ID Encoder + Redis Key-Value Store |
| **FR-18** | Web Crawler for Catalogue Enrichment | Asynchronous worker crawling mock external library/publisher endpoints to import book summaries, ratings, and external availability without duplicates. | Multi-threaded Web Crawler with Deduplication Set |
| **FR-19** | Sharding of Member & Book Data | Partition high-cardinality collections (members, books, copies, loans) across database shards using carefully chosen shard keys. | MongoDB Sharding & Application Shard Routing |
| **FR-20** | Consistent Hashing Request Routing | Hash ring with virtual nodes (VNodes) for balancing requests across library service instances and measuring remapping overhead on node addition/removal. | Consistent Hash Ring with MD5/Murmur3 + Virtual Nodes |
| **FR-21** | API Rate Limiting | Protect abuse-prone endpoints (login, search, crawler triggers) with Redis Token Bucket rate limiting returning HTTP 429. | Redis Lua Atomic Token Bucket Rate Limiter |
| **FR-22** | System Metrics Collection | Real-time capture of throughput, P50/P95/P99 latency, cache hit/miss ratio, rejection rates, shard distribution. | Spring Actuator + Custom Metrics Registry |
| **FR-23** | Administrative Catalogue Management | Add, update, archive titles, add individual physical copies with barcodes. | Role-protected REST Endpoints |
| **FR-24** | Health Checks & Diagnostics | Probes for MongoDB, Redis, application nodes, and cluster health status. | Actuator Health Endpoint |

---

## 0.2 Non-Functional Requirements (NFR)

1. **Scalability**: The system must sustain throughput scaling from 10 to 100+ concurrent virtual users without race conditions or memory exhaustion.
2. **Data Consistency**: Strict serializability for copy borrowing (exactly one borrower per physical copy) using atomic conditional updates.
3. **Low Latency**: Autocomplete and cached searches must respond in under 15ms at P95.
4. **Availability & Fault Tolerance**: Graceful degradation if Redis is down (fail-open to database for queries, fail-closed for authentication protection where appropriate).
5. **Idempotency**: Retried payment requests or duplicate returns must yield identical results without duplicate state mutation.
6. **Security**: BCrypt password hashing (strength 10), signed JWTs (HMAC-SHA256), sanitized search queries, no secrets committed.
7. **Observability**: Live metrics dashboard in the web UI displaying component statistics and benchmark results.

---

## 0.3 Acceptance Traceability Matrix

| Req ID | Target Module | API / Workflow | Verification Test | Status |
|--------|---------------|----------------|-------------------|--------|
| FR-01, 02 | `auth`, `member` | `/api/auth/register`, `/api/auth/login` | Unit + Auth Integration Test | Ready to Implement |
| FR-03 | `security` | Role-based filters | Admin vs Member 403 checks | Ready to Implement |
| FR-04, 05 | `catalogue`, `search` | `/api/books`, `/api/books/search` | Search & Filter Integration Test | Ready to Implement |
| FR-06 | `catalogue` | `/api/books/{id}` | Enrichment validation test | Ready to Implement |
| FR-07 | `circulation` | `/api/books/{id}/copies` | Copy availability test | Ready to Implement |
| FR-08 | `circulation` | `/api/circulation/borrow` | Concurrent Borrowing Stress Test (50 threads) | Ready to Implement |
| FR-09, 10 | `circulation` | `/api/circulation/return` | Duplicate return test + Overdue calculation | Ready to Implement |
| FR-11, 12 | `fines`, `payments` | `/api/fines`, `/api/payments` | Idempotent payment test | Ready to Implement |
| FR-13, 14, 15 | `reservations` | `/api/reservations` | Queue FIFO order test | Ready to Implement |
| FR-16 | `autocomplete` | `/api/autocomplete?q=...` | Trie Prefix Precision@k test | Ready to Implement |
| FR-17 | `shorturl` | `/s/{code}`, `/api/urls` | Base62 resolution and redirection test | Ready to Implement |
| FR-18 | `crawler` | `/api/crawler/jobs` | Mock endpoint import & deduplication test | Ready to Implement |
| FR-19 | `sharding` | Shard Router | Chunk & record distribution check | Ready to Implement |
| FR-20 | `consistenthash` | Node Ring | Remapping & imbalance calculation test | Ready to Implement |
| FR-21 | `ratelimit` | Filter / Interceptor | 429 Rejection rate benchmark | Ready to Implement |
| FR-22, 24 | `metrics` | `/api/metrics`, `/actuator/health` | Health & metrics endpoint tests | Ready to Implement |

---

## 0.4 Workspace & Environment Audit

- **Operating System**: Windows 11 (amd64)
- **Java Runtime**: Eclipse Adoptium OpenJDK 17.0.13 LTS
- **Build Tool**: Apache Maven 3.9.14
- **Node.js**: v22.13.0, npm 10.9.2
- **Python**: Python 3.11.9 (with `requests`, `matplotlib`, `pandas`, `seaborn`, `pymupdf` installed)
- **MongoDB**: Native MongoDB Server v8.0.4 actively running on `localhost:27017`
- **Redis**: Available via Docker container (or embedded/native adapter fallback)
- **Course Context**: Coursework Lab UCS3513, exercises Ex 1 through Ex 8 completed and verified.
