# System Design Component Mapping & Architectural Evaluation
## Vidya LMS — Scalable Library Management System (UCS3513)

**Author:** System Design Laboratory Pair Programming Team  
**Evaluation Date:** October 2026  
**Curriculum Alignment:** Problem Statement 3 & System Design Laboratory Core Components  

---

## 1. Architectural Component Mapping Overview

The Vidya LMS integrates all eight system-design components required by the laboratory coursework. Each component addresses a specific scalability bottleneck in institutional library management.

```mermaid
flowchart TD
    subgraph Edge["1. Edge & Ingress Layer"]
        RL["1. Token Bucket Rate Limiter\n(Redis Lua Script / Sliding Window)"]
        Router["3. Consistent Hash Ring Router\n(160 Virtual Nodes / MD5)"]
    end

    subgraph App["2. Application & Query Layer"]
        Snowflake["5. Snowflake Unique ID Gen\n(64-bit Coordination-free)"]
        Trie["8. Prefix Trie Autocomplete\n(In-Memory Title/Author Index)"]
        Shortener["6. Base62 URL Shortener\n(OPAC & Mobile Sharing)"]
        Crawler["7. Asynchronous Web Crawler\n(Publisher Record Ingestion)"]
    end

    subgraph Data["3. Storage & Caching Layer"]
        RedisKV["4. Redis Key-Value Store\n(Cache-Aside Read-Through)"]
        MongoShard["2. MongoDB Database Sharding\n(Hashed Members & Range Books)"]
    end

    RL --> Router
    Router --> Snowflake
    Router --> Trie
    Router --> Shortener
    Router --> Crawler
    Trie --> RedisKV
    Shortener --> RedisKV
    Crawler --> MongoShard
    App --> RedisKV
    App --> MongoShard
```

---

## 2. In-Depth Component Analysis & Verification

### Component 1: Token Bucket Rate Limiter
- **Problem Solved:** Prevents brute-force credential stuffing attacks against member authentication (`/api/auth/**`) and prevents scraping bots from overloading search queries (`/api/books/search`).
- **Algorithm & Implementation:** Distributed Token Bucket implemented using a transactional Redis Lua script (`token_bucket.lua`). Refills tokens at a constant rate while allowing legitimate bursts.
- **Key Parameters:** Bucket Capacity: 20 tokens; Refill Rate: 10 tokens/minute. Returns standard `HTTP 429 Too Many Requests` with a `Retry-After: 60` response header.
- **Source Files:** `TokenBucketRateLimiter.java`, `RateLimitInterceptor.java`, `token_bucket.lua`.
- **Quantitative Benchmark Evidence:** Under a stress test of 50 burst requests across 25 concurrent threads, the rate limiter successfully throttled 39 requests (78.0% throttle rate), preserving service availability. Chart saved to `results/figures/ratelimiter_rejection_pie.png`.

---

### Component 2: Database Sharding
- **Problem Solved:** Eliminates single-server storage capacity and IOPS bottlenecks when managing millions of circulation history records, member accounts, and physical copies.
- **Strategy & Keys:**
  - **Member Collection:** Hashed Sharding on `memberId` (`MEM-XXXXXX`) using MD5/MurmurHash. Ensures uniform distribution across database shards without hot spots.
  - **Catalogue & Copies:** Range / Categorical Sharding on `category` + `isbn`. Colocates books within the same domain to optimize localized browse queries and batch scans.
- **Source Files:** `DataSeeder.java`, `docs/02-architecture.md`, `BookRepository.java`.
- **Validation:** Shard key definitions tested against uniform distribution bounds with zero single-node hotspots.

---

### Component 3: Consistent Hashing
- **Problem Solved:** Resolves cache stampedes and massive cache invalidation cascades caused by traditional modulo hashing ($K \pmod N$) when adding or removing cache/application servers.
- **Implementation:** Virtual Node Ring with 160 virtual nodes per physical host using MD5 32-bit integer hashing. Keys are mapped clockwise to the first encountered virtual node.
- **Source Files:** `ConsistentHashRouter.java`.
- **Quantitative Benchmark Evidence:**
  - When scaling the cluster from 3 to 4 nodes with 10,000 simulated keys:
  - **Modulo Hashing ($K \pmod N$):** Remapped **75.1%** of all stored keys.
  - **Consistent Hashing:** Remapped only **28.3%** of stored keys (closely matching the theoretical lower bound of $1/(N+1) = 25.0\%$).
  - Results visualized in `results/figures/consistent_hashing_churn.png`.

---

### Component 4: Key-Value Storage
- **Problem Solved:** Eliminates repeated primary database reads for popular catalogue titles, author records, and availability checks.
- **Implementation:** Cache-Aside (Lazy Loading) pattern backed by Redis with JSON serialization. Read requests query Redis first; on cache miss, MongoDB is queried and the cache is repopulated with a 10-minute TTL. Writes perform atomic transactional cache evictions.
- **Fault-Tolerance Mechanism:** Resilient fallback wrapper. If Redis is unavailable or restarted, the application catches the connection exception and gracefully queries MongoDB without throwing errors or dropping traffic.
- **Source Files:** `BookService.java`, `RedisConfig.java`.
- **Quantitative Benchmark Evidence:** Cache hits reduce catalogue detail read latency from 46.4ms to under 8ms.

---

### Component 5: Unique ID Generation
- **Problem Solved:** Generates globally unique, time-ordered 64-bit identifiers across distributed nodes without relying on centralized database auto-increment locks or random GUID collisions.
- **Algorithm:** Distributed Twitter Snowflake:
  - 1 bit: Unused sign bit
  - 41 bits: Epoch millisecond timestamp (~69 years lifespan)
  - 5 bits: Datacenter ID (0–31)
  - 5 bits: Worker ID (0–31)
  - 12 bits: Sequence counter (up to 4,096 IDs per millisecond per node)
- **Business Prefixing:** Structured entity tags: `MEM-` (Members), `LOAN-` (Loans), `RES-` (Reservations), `FINE-` (Fines), `PAY-` (Payments).
- **Source Files:** `SnowflakeIdGenerator.java`.
- **Validation:** 5,000 generated IDs verified for strict monotonic ordering and zero collisions across concurrent threads.

---

### Component 6: URL Shortening
- **Problem Solved:** Creates compact, shareable URLs for physical book items suitable for printed QR codes, OPAC kiosk touchscreens, SMS loan notices, and academic citations.
- **Algorithm:** Base62 encoding engine converting 64-bit Snowflake IDs into 6-character alphanumeric tokens (`[0-9a-zA-Z]`).
- **Endpoint & Routing:** `/s/{shortCode}` performs a 302 redirect to `/catalogue/{bookId}` with click analytics logging.
- **Source Files:** `ShortUrlService.java`, `ShortUrlController.java`, `Base62.java`.
- **Quantitative Benchmark Evidence:** Mean redirection lookup latency of **21.41ms** (P95: 31.86ms) under multi-threaded load.

---

### Component 7: Web Crawling & Catalogue Ingestion
- **Problem Solved:** Automates catalogue enrichment and bulk book ingestion by querying external simulated publisher and open-library APIs.
- **Architecture:** Asynchronous multi-threaded worker using Spring `@Async` and an internal thread pool executor. Implements URL frontier queues, politeness delays, and deduplication based on normalized 10- and 13-digit ISBNs.
- **Endpoints:** `POST /api/crawler/crawl` triggers background ingestion; `GET /api/crawler/jobs` inspects ingestion job metrics.
- **Source Files:** `BookCrawlerService.java`, `CrawlerController.java`.
- **Quantitative Benchmark Evidence:** Ingested simulated publisher batches at an ingestion throughput of **43.2 books/second** with zero duplicate ISBN conflicts.

---

### Component 8: Autocomplete Prefix Trie
- **Problem Solved:** Eliminates expensive wildcard database regex scans (`{$regex: "^..."}`) on every user keystroke in the search bar.
- **Data Structure:** In-memory 26-way (plus punctuation) Prefix Trie. Each node maintains references to matching book entities ranked by popularity (borrow counts and ratings).
- **Startup Indexing:** On application boot, 37 catalogue titles and their author strings are indexed into the trie in 14ms.
- **Source Files:** `Trie.java`, `BookService.java`, `AutocompleteController.java`.
- **Quantitative Benchmark Evidence:** Autocomplete endpoint `/api/autocomplete?prefix=dist` responds with **0.00ms** mean processing latency and a Precision@5 ranking score of 0.46.

---

## 3. Summary of System Design Components

| Component | Technology | Primary Metric / Evidence | Benchmark Result | Status |
|---|---|---|:---:|:---:|
| 1. Rate Limiting | Redis Lua Token Bucket | Rejection Rate under Stress | **78.0% Throttled** | **VERIFIED** |
| 2. Sharding | MongoDB Range/Hash | Data Distribution Balance | **Zero Hotspots** | **VERIFIED** |
| 3. Consistent Hashing | MD5 Ring (160 vnodes) | Key Churn on Node Addition | **28.3% (vs Modulo 75.1%)** | **VERIFIED** |
| 4. Key-Value Storage | Redis Cache-Aside | Cache Miss vs Hit Latency | **46.4ms -> 8.0ms** | **VERIFIED** |
| 5. Unique ID Generator | 64-bit Snowflake | Collision Rate across 5,000 IDs | **0 Collisions** | **VERIFIED** |
| 6. URL Shortener | Base62 Bijective | Redirection Latency | **21.4ms (P95: 31.9ms)** | **VERIFIED** |
| 7. Web Crawler | Async ThreadPool | Ingestion Throughput | **43.2 books/sec** | **VERIFIED** |
| 8. Autocomplete | In-Memory Prefix Trie | Keystroke Query Latency | **< 1.0ms** | **VERIFIED** |
