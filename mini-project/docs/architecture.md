# System Architecture & Technical Design Specification
## Problem Statement 3: Scalable Library Management System
**Course**: UCS3513 — System Design Laboratory  
**Document**: Architectural Design & Technical Specification  

---

## 1. High-Level Component Architecture

```mermaid
flowchart TD
    Client["Client Devices (React 18 SPA / Mobile / OPAC Kiosks)"] --> Nginx["Nginx Reverse Proxy & Gateway (:80)"]
    Nginx --> RL["Token Bucket Rate Limiter (Lua Script in Redis)"]
    RL --> SpringApp["Spring Boot 3.3 Application Cluster (:8080)"]

    subgraph ServiceLayer["Core Domain Services"]
        AuthSvc["Auth & Member Service (JWT + BCrypt)"]
        CatSvc["Catalogue & Prefix Trie Autocomplete"]
        CircSvc["Circulation Engine (Atomic Claim)"]
        ResSvc["Reservation Queue Service (FIFO)"]
        PaySvc["Idempotent Payment Engine (UUID Keys)"]
        ShortSvc["Base62 URL Shortener"]
        CrawlSvc["Asynchronous Web Crawler Pool"]
        HashRing["Consistent Hash Router (160 vnodes)"]
    end

    SpringApp --> ServiceLayer

    subgraph DataStorage["Persistence & Caching Tier"]
        RedisKV[("Redis Key-Value Cache & Rate Limiting")]
        SnowflakeEngine["Snowflake 64-bit ID Engine (Worker 1, DC 1)"]
        MongoDB[("MongoDB 8.0 Primary Store")]
        Shard1[("Shard 1: Members (Hashed memberId)")]
        Shard2[("Shard 2: Books (Category Compound Index)")]
    end

    CatSvc <--> RedisKV
    CatSvc <--> MongoDB
    CircSvc <--> MongoDB
    PaySvc <--> MongoDB
    ResSvc <--> MongoDB
    SpringApp --> SnowflakeEngine
    CrawlSvc -.-> MockExt["Mock External Publisher API"]
```

---

## 2. Request Lifecycle: Book Search & Autocomplete Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as Library Member
    participant UI as React UI (AutocompleteInput)
    participant Gateway as API Gateway / Rate Limiter
    participant Trie as Prefix Trie Engine (In-Memory)
    participant Redis as Redis Key-Value Store
    participant Mongo as MongoDB Storage Engine

    User->>UI: Types keystroke 'dist'
    UI->>Gateway: GET /api/autocomplete?prefix=dist&limit=5
    Gateway->>Trie: Traverse prefix node 'd'->'i'->'s'->'t'
    Trie-->>UI: Top-5 ranked suggestions (< 1 ms latency)
    
    User->>UI: Selects 'Distributed Systems'
    UI->>Gateway: GET /api/books/search?q=Distributed&mode=optimized
    Gateway->>Redis: Check search cache for query key
    alt Cache Hit
        Redis-->>UI: Cached Book Page JSON (< 2 ms)
    else Cache Miss
        Gateway->>Mongo: Query books collection (Indexed text/category filter)
        Mongo-->>Gateway: Authoritative Book Records
        Gateway->>Redis: Set cache key with 10-minute TTL
        Gateway-->>UI: 200 OK (Paginated Books Content)
    end
```

---

## 3. Authentication & Rate-Limiting Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Script
    participant Limiter as Token Bucket Rate Limiter
    participant Auth as AuthService & Security Filter
    participant DB as MongoDB Member Store

    Client->>Limiter: POST /api/auth/login {email, password}
    Limiter->>Limiter: Check Token Bucket (capacity=10, refill=2/s)
    alt Bucket Empty (Tokens < 1)
        Limiter-->>Client: 429 Too Many Requests (Retry-After: 5s)
    else Token Acquired
        Limiter->>Auth: Pass request to AuthenticationManager
        Auth->>DB: findByEmail(email)
        DB-->>Auth: Member Document & BCrypt Hash
        Auth->>Auth: passwordEncoder.matches(password, hash)
        alt Password Invalid
            Auth-->>Client: 401 Unauthorized ("Bad credentials")
        else Password Valid
            Auth->>Auth: Generate HMAC-SHA384 JWT Token
            Auth-->>Client: 200 OK {token, memberId, role, name}
        end
    end
```

---

## 4. Borrowing & Returning Concurrency Transaction Flow

```mermaid
sequenceDiagram
    autonumber
    actor M1 as Member 1 (Thread A)
    actor M2 as Member 2 (Thread B)
    participant API as Circulation Controller & Service
    participant Mongo as MongoDB Engine
    participant Redis as Redis Cache

    Note over M1, M2: Simultaneous race for final available copy of Book B1
    par Concurrent HTTP Requests
        M1->>API: POST /api/circulation/borrow {bookId: B1}
        M2->>API: POST /api/circulation/borrow {bookId: B1}
    end

    par Atomic Execution in Mongo
        API->>Mongo: findAndModify(bookId=B1, status=AVAILABLE -> BORROWED) [M1]
        API->>Mongo: findAndModify(bookId=B1, status=AVAILABLE -> BORROWED) [M2]
    end

    Mongo-->>API: Returns Claimed Copy Document C-1 [M1]
    Mongo-->>API: Returns null (no copy matches AVAILABLE) [M2]

    API->>Redis: Evict cache:book:B1
    API-->>M1: 200 OK (Loan Created, Due Date Issued)
    API-->>M2: 409 Conflict ("All copies checked out. Reservation available.")

    Note over M1, API: Member 1 returns the copy
    M1->>API: POST /api/circulation/return {copyIdOrBarcode: C-1}
    API->>Mongo: Update copy status to AVAILABLE, loan to RETURNED
    API->>Redis: Evict cache:book:B1
    API-->>M1: 200 OK (Copy Returned)

    Note over M1, API: Member 1 retries return (Network Retry)
    M1->>API: POST /api/circulation/return {copyIdOrBarcode: C-1}
    API->>Mongo: Check active loan -> Not found, copy already AVAILABLE
    API-->>M1: 200 OK (status: "ALREADY_RETURNED", 0 duplicate fines)
```

---

## 5. Reservation Queue Handover Flow

```mermaid
flowchart TD
    ReturnCopy["Member returns borrowed physical copy"] --> CheckQueue{"Active PENDING reservations exist for Book?"}
    CheckQueue -- Yes --> FindHead["Query first reservation ordered by queuePosition ASC (FIFO)"]
    FindHead --> AllocateCopy["Set reservation status = READY_FOR_PICKUP<br/>Set pickupDeadline = now + 48 hours<br/>Set copy status = RESERVED_FOR_PICKUP"]
    AllocateCopy --> NotifyMember["Trigger reservation readiness notification"]
    CheckQueue -- No --> ShelfAvailable["Set copy status = AVAILABLE<br/>Restore library shelf inventory counter"]
```

---

## 6. Cache-Aside Read & Invalidation Flow

```mermaid
flowchart LR
    Req["GET /api/books/{id}"] --> CheckCache{"Redis Cache Hit?"}
    CheckCache -- Yes (Key Exists) --> ReturnCached["Deserialize JSON & Return (< 2ms)"]
    CheckCache -- No (Key Missing) --> ReadDB["Read Authoritative Document from MongoDB"]
    ReadDB --> WriteCache["Write JSON to Redis with TTL = 600s"]
    WriteCache --> ReturnDB["Return Book Detail Response"]

    Mutation["Borrow / Return / Add Copies"] --> UpdateDB["Update MongoDB State"]
    UpdateDB --> Invalidate["redisTemplate.delete(cache:book:{id})"]
```

---

## 7. Deployment Architecture

```mermaid
graph TD
    subgraph Host["Host Operating System (Windows 11 / Docker Host)"]
        subgraph FrontendTier["Presentation Tier"]
            ViteDev["Vite Dev Server (:5173) / Nginx (:80)"]
        end
        subgraph BackendTier["Application Tier"]
            SpringBoot["Spring Boot Application (PID 14176 :8080)"]
        end
        subgraph StorageTier["Data & Cache Tier"]
            MongoDB["MongoDB Community Server (:27017)"]
            RedisInst["Redis Server (:6379) / In-Memory Fallback"]
        end
    end

    ViteDev -->|REST & Short URLs| SpringBoot
    SpringBoot --> MongoDB
    SpringBoot --> RedisInst
```

---

## 8. Scaling and Failure Behavior

1. **Horizontal Backend Scaling**:
   - The Spring Boot backend is completely stateless; JWT tokens encapsulate authentication context.
   - Any number of Spring Boot instances can run behind Nginx or a Consistent Hash router.
2. **Consistent Hash Cluster Ring**:
   - Application requests are hashed using MD5 onto a 160-virtual-node ring.
   - When a server instance fails or an instance is added, only $\approx \frac{1}{N+1}$ fraction of cached keys migrate, preventing cascading cache stampedes.
3. **Redis Degradation Mode**:
   - If Redis becomes unreachable, `TokenBucketRateLimiter` degrades to thread-safe local `ConcurrentHashMap` buckets.
   - `BookService` degrades to direct MongoDB reads without throwing 500 errors.
4. **Zero Double-Borrowing Invariant**:
   - The system uses MongoDB's atomic `findAndModify` with an `AVAILABLE` status filter. Under 20 concurrent threads competing for 1 copy, exactly 1 succeeds and 19 receive HTTP 409 Conflict.
