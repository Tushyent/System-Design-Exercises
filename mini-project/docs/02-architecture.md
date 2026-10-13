# Phase 1: System Architecture and Design
## Problem Statement 3: Scalable Library Management System

### 1. High-Level Architecture

The Scalable Library Management System is architected as a layered, modular distributed system designed for high availability, low latency, and linear horizontal scalability.

```mermaid
graph TD
    User["Web Browser / Client (React + TypeScript)"]
    Proxy["Nginx / Load Balancer & Consistent Hash Router"]
    
    subgraph "Application Layer (Spring Boot 3)"
        AppNode1["Library Service Instance 1"]
        AppNode2["Library Service Instance 2"]
        
        subgraph "Core Modules"
            AuthMod["Authentication & RBAC (JWT)"]
            CircMod["Circulation (Borrow / Return / Lock)"]
            ResMod["Reservation Queue Manager"]
            FineMod["Fine & Idempotent Payments"]
            AutoMod["Autocomplete Engine (Trie)"]
            ShortMod["Base62 URL Shortener"]
            CrawlMod["Web Crawler Service"]
            IdGen["Snowflake Unique ID Generator"]
        end
    end
    
    subgraph "Caching & Fast Key-Value Layer"
        RedisCluster[("Redis Key-Value Store\n• Popular Book Cache\n• Availability Cache\n• Rate Limiter State\n• Idempotency Tokens")]
    end
    
    subgraph "Distributed Data Layer (MongoDB)"
        MongoRouter["mongos Query Router"]
        ConfigServer["Config Replica Set"]
        Shard1[("MongoDB Shard 1\n• Members / Books Chunk A\n• Loans / Fines")]
        Shard2[("MongoDB Shard 2\n• Members / Books Chunk B\n• Reservations")]
    end
    
    subgraph "Simulated External Sources"
        MockCatalog["Mock External Library / Publisher API"]
    end

    User --> Proxy
    Proxy --> AppNode1
    Proxy --> AppNode2
    
    AppNode1 --> RedisCluster
    AppNode2 --> RedisCluster
    
    AppNode1 --> MongoRouter
    AppNode2 --> MongoRouter
    
    MongoRouter --> ConfigServer
    MongoRouter --> Shard1
    MongoRouter --> Shard2
    
    CrawlMod -.->|Poll & Enrich| MockCatalog
```

---

### 2. Component Interaction & Data Flow

```mermaid
sequenceDiagram
    autonumber
    actor Member as Library Member
    participant Gateway as API Gateway / Rate Limiter
    participant CircService as Circulation Service
    participant IdGen as Snowflake ID Gen
    participant Redis as Redis Cache
    participant Mongo as MongoDB Sharded Cluster
    
    Member->>Gateway: POST /api/circulation/borrow {bookId, memberId}
    Gateway->>Redis: Check Rate Limit (Token Bucket)
    alt Rate Limit Exceeded
        Gateway-->>Member: 429 Too Many Requests (Retry-After: 60)
    else Rate Limit OK
        Gateway->>CircService: Execute Borrow Workflow
        CircService->>Mongo: Check Member Status & Fines
        alt Has Blocking Fines
            CircService-->>Member: 400 Bad Request (Outstanding fines exceed limit)
        else Eligible
            CircService->>Mongo: Find & Atomically Claim Copy {status: "AVAILABLE"}
            alt No Copies Available
                CircService-->>Member: 409 Conflict (All copies loaned; Prompt Reservation)
            else Copy Claimed
                CircService->>IdGen: Generate 64-bit Snowflake loanId
                CircService->>Mongo: Insert Loan Document (loanId, copyId, memberId, dueDate)
                CircService->>Redis: Invalidate availability cache for bookId
                CircService-->>Member: 200 OK (Loan Created, Due in 14 days)
            end
        end
    end
```

---

### 3. Return & Reservation Queue Handover Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Member as Returning Member
    participant Circ as Circulation Service
    participant ResQueue as Reservation Queue
    participant FineService as Fine Calculation
    participant Mongo as MongoDB Shards
    participant Redis as Redis Cache
    
    Member->>Circ: POST /api/circulation/return {copyId}
    Circ->>Mongo: Atomic Query Loan {copyId, status: "ACTIVE"}
    alt Loan Already Returned
        Circ-->>Member: 200 OK (Already returned - Idempotent response)
    else Active Loan Found
        Circ->>Mongo: Mark Loan RETURNED with timestamp
        Circ->>FineService: Check overdue (ReturnDate > DueDate)
        opt Overdue Days > 0
            FineService->>Mongo: Create Fine Record ($1.50/day overdue)
        end
        Circ->>ResQueue: Peek Next Active Reservation for Book
        alt Active Reservation Exists
            ResQueue->>Mongo: Assign Copy to Member 1, State: RESERVED_FOR_PICKUP
            Circ-->>Member: 200 OK (Returned; Assigned to Waiting Reservation)
        else No Reservations
            Circ->>Mongo: Update Copy Status: AVAILABLE
            Circ->>Redis: Invalidate availability cache
            Circ-->>Member: 200 OK (Returned successfully; Copy Available)
        end
    end
```

---

### 4. Sharding and Request Routing Architecture

```mermaid
graph LR
    subgraph "Consistent Hashing Ring"
        Ring(("Virtual Node Ring\n0 ............. 2^32-1"))
        NodeA["Node-A (VNodes: 1, 4, 7)"]
        NodeB["Node-B (VNodes: 2, 5, 8)"]
        NodeC["Node-C (VNodes: 3, 6, 9)"]
    end
    
    subgraph "Shard Key Distribution"
        BookKey["Book ID: b1001\nHash: 0x3fa..."] --> Shard1["Shard 1 (Chunks: [MinKey, 0x80...])"]
        BookKey2["Book ID: b1002\nHash: 0xb9e..."] --> Shard2["Shard 2 (Chunks: [0x80..., MaxKey])"]
    end
```
