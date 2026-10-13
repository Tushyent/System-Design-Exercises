# Phase 1: Component Mapping and Architectural Justification
## Problem Statement 3: Scalable Library Management System

### Introduction
The SSN System Design Laboratory problem statement provides 8 core system-design building blocks without explicit mapping:
1. **Rate Limiter**
2. **Sharding**
3. **Consistent Hashing**
4. **Key-Value Store**
5. **Unique ID Generator**
6. **URL Shortener**
7. **Web Crawler**
8. **Autocomplete**

Below is the formal mapping, technical rationale, alternative evaluations, and architectural trade-offs for each component in the Library Management System.

---

### Component 1: Rate Limiter
- **Mapped Library Functionality**:
  - Member authentication & registration (`/api/auth/*`) to prevent credential stuffing and brute-force attacks.
  - Public search & autocomplete (`/api/books/search`, `/api/autocomplete`) to protect MongoDB and search trie from Denial of Service (DoS) scraping.
  - Crawler execution triggers (`/api/crawler/trigger`) to prevent unbounded background job spawning.
- **Selected Algorithm**: Redis-backed **Token Bucket** algorithm using atomic Lua script.
- **Justification**: Token Bucket allows burst capacity while maintaining a smooth sustained replenishment rate. By storing tokens and last-refill timestamp in Redis, rate limits are enforced consistently across multiple application nodes without race conditions.
- **Alternatives Considered**:
  - *Fixed Window Counter*: Suffers from 2x burst vulnerability at boundary edges.
  - *Sliding Window Log*: High Redis memory overhead due to storing timestamp sets per request.
- **Policy**:
  - Authentication: 5 requests / minute per client IP (fail-closed to protect security).
  - Search / Autocomplete: 30 requests / minute per IP or authenticated user (fail-open on Redis timeout to maintain search availability).

---

### Component 2: Sharding
- **Mapped Library Functionality**:
  - Storage of high-volume **Members**, **Books**, **BookCopies**, and **Loans/Transactions**.
- **Selected Architecture & Strategy**:
  - **Books Collection**: Sharded on Hashed Book ID (`{ _id: "hashed" }`) or Range on Category + Book ID. Hashed sharding provides uniform write and read distribution across shards, preventing hotspots during mass imports.
  - **Members Collection**: Sharded on Hashed Member ID (`{ memberId: "hashed" }`), distributing member lookups and login checks.
  - **Loans Collection**: Sharded by `{ memberId: 1, loanId: 1 }` to preserve read locality when retrieving all active loans and history for a given member.
- **Justification**: As library members grow into tens of thousands and catalogue expands with millions of physical copies and loan records, a single node database encounters memory exhaustion and I/O bottlenecks. Horizontal sharding splits data into discrete chunks across physical/logical shards.

---

### Component 3: Consistent Hashing
- **Mapped Library Functionality**:
  - Distribution of requests across multiple stateless Library Service application nodes or cache storage nodes.
  - Application-level routing of member sessions or partitioned circulation queues.
- **Selected Algorithm**: **Consistent Hash Ring with Virtual Nodes (VNodes)**.
- **Justification**:
  - In traditional modulo hashing (`node = hash(key) % N`), adding or removing a single node invalidates nearly `N / (N + 1)` of the mapped keys, causing massive cache stampedes and server re-allocations.
  - Consistent hashing on a 32-bit/64-bit integer ring ensures that adding or removing a node only migrates `K / N` keys on average (minimal disruption).
  - Using 100+ virtual nodes per physical node smooths out standard deviation of load distribution, eliminating clustering hotspots.

---

### Component 4: Key-Value Store (Redis)
- **Mapped Library Functionality**:
  - **Catalogue & Book Details Cache**: Cache frequently viewed popular books and book metadata.
  - **Search Result & Autocomplete Cache**: Cache frequent query results with short TTLs.
  - **Availability Cache**: Fast reading of available copy counts, coupled with **immediate cache invalidation** on borrow/return/reservation events.
  - **Idempotency Store**: Store transaction idempotency tokens for simulated fine payments.
- **Consistency Rules**:
  - **Authoritative Database Source**: The physical `BookCopy` document in MongoDB is the sole authoritative state during borrow operations. The cache is used strictly for fast reads (`read-through` / `cache-aside`).
  - Upon a successful borrow or return, the cache key `book:availability:{bookId}` is invalidated synchronously.

---

### Component 5: Unique ID Generator
- **Mapped Library Functionality**:
  - Generation of unique, 64-bit time-sortable identifiers for:
    - Loans (`loanId`)
    - Reservations (`reservationId`)
    - Physical Book Copy barcodes (`copyId`)
    - Payment transaction references (`paymentId`)
- **Selected Algorithm**: **Snowflake-style Distributed 64-bit ID Generator**:
  - `1 bit`: Unused sign bit (0).
  - `41 bits`: Millisecond timestamp since custom epoch (~69 years lifetime).
  - `10 bits`: Worker / Node ID (supports up to 1024 distributed generator instances).
  - `12 bits`: Sequence counter (supports up to 4096 unique IDs per millisecond per node).
- **Guarantees**:
  - Monotonically increasing / time-sortable.
  - Collision-free across concurrent threads and distributed nodes without database auto-increment locking.
  - Handles clock rollback safely (spinning or failing with explicit exception).

---

### Component 6: URL Shortener
- **Mapped Library Functionality**:
  - Generating compact, shareable URLs for book catalogue items (e.g., `/s/bK7x9`) and reservation confirmation links for SMS/Email notices.
- **Selected Algorithm**:
  - Unique ID to **Base62 Encoding** (`[0-9a-zA-Z]`).
  - Redis cache for sub-millisecond redirection resolution.
  - MongoDB persistent store for long-term mapping and click analytics.

---

### Component 7: Web Crawler
- **Mapped Library Functionality**:
  - External metadata enrichment: Crawling simulated external book sources (e.g., mock OpenLibrary / publisher catalogues) to enrich library records with synopsis, publisher details, community ratings, and cover URLs.
- **Architecture**:
  - Asynchronous background worker using a worker pool.
  - Politeness policy: Configurable delay, respects concurrency limits.
  - Deterministic deduplication using MD5/SHA-256 fingerprinting on ISBN and normalized Title + Author.
  - Non-blocking: External source unavailability does not degrade core local library circulation.

---

### Component 8: Autocomplete
- **Mapped Library Functionality**:
  - Instant title, author, and category search suggestions as users type in the catalogue search bar.
- **Selected Data Structure**:
  - In-Memory **Prefix Trie** combined with Redis prefix caching.
  - Case-insensitive, prefix-matching, Top-K ranking based on book popularity/borrow counts.
  - Bounded results (default `limit = 8`).
  - Dynamic updates: Trie is refreshed when new books are created or crawled.
- **Performance Objective**: Sub-10ms response time at P95 under concurrent search load.

---

### Summary Matrix

| Component | Library Feature Mapped | Primary Metric Evaluated | Baseline Comparison |
|---|---|---|---|
| **Rate Limiter** | Auth, Search, Crawler APIs | Rejection rate (HTTP 429), latency overhead | Unrestricted endpoints |
| **Sharding** | Books, Members, Loans data | Chunk distribution, query targeting | Single monolithic collection |
| **Consistent Hashing** | Server / Cache routing | Load balance imbalance %, key remapping % | Modulo hashing ($K \pmod N$) |
| **Key-Value Store** | Popular books & availability | Read latency, hit/miss ratio, invalidation correctness | Direct MongoDB disk reads |
| **Unique ID Gen** | Loans, Reservations, Payments | Throughput (IDs/sec), duplicate count (0) | UUIDv4 / Mongo ObjectId |
| **URL Shortener** | Book share links | Redirection latency, collision rate | Plain long URL params |
| **Web Crawler** | Catalogue enrichment | Crawl throughput (books/s), deduplication rate | Manual single-item entry |
| **Autocomplete** | Search bar live suggestions | Suggestion latency, Precision@k, Recall@k | DB regex wildcard search |
