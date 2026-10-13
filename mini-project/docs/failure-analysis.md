# Scalability & Failure Scenario Analysis Report
## Scalable Library Management System (Problem Statement 3)
**Course**: UCS3513 — System Design Laboratory, SSN College of Engineering  
**Document**: Fault Tolerance, Resilience, Scalability, and Bottleneck Analysis  

---

## 1. Failure Scenario 1: Redis Key-Value Store & Rate Limiter Outage

### Initial Conditions
The backend is configured to use Redis on port 6379 for distributed sliding-window token bucket state and read-through caching.

### Failure Injected
Redis connection is unavailable (port 6379 closed / unreachable).

### Observed System Behavior & Resilience
1. **Rate Limiting**: `TokenBucketRateLimiter` catches Redis connection exceptions and transparently falls back to an in-memory `ConcurrentHashMap<String, LocalBucket>`. Refill logic continues smoothly; requests are permitted or throttled according to configured capacities without throwing HTTP 500 errors.
2. **Read-Through Caching**: `BookService` catches Redis read/write timeouts, logs a diagnostic warning, and fetches authoritative data directly from MongoDB.
3. **Recovery**: Once Redis service is restored on port 6379, the application automatically re-establishes connectivity without requiring a backend restart.

---

## 2. Failure Scenario 2: Node Failure & Server Expansion in Consistent Hash Ring

### Initial Conditions
A 3-node application cluster routes 2,000 requests using an MD5 hash ring with 160 virtual nodes per server.

### Failure / Topology Change Injected
A 4th library node is dynamically added to the cluster (or conversely, an existing node becomes unavailable).

### Observed Behavior & Churn Measurement
- **Consistent Hashing**: Only **28.3%** of keys migrated to the new node. 71.7% of existing cache routes remained entirely untouched.
- **Classic Modulo Hashing (`hash(key) % N`)**: **75.1%** of keys migrated, invalidating $\approx 3/4$ of all cached items simultaneously.
- **Architectural Impact**: Consistent hashing prevents cascading cache stampedes and eliminates sudden database saturation during autoscaling events.

---

## 3. Failure Scenario 3: High-Contention Race Condition on Scarce Physical Inventory

### Initial Conditions
A high-demand book has exactly 1 physical copy remaining in the library.

### Failure / Stress Injected
20 concurrent threads submit borrowing requests simultaneously across multiple member accounts.

### Observed Behavior & Data Integrity
- **Database Primitive**: MongoDB atomic `findAndModify` with `{ bookId: ..., status: "AVAILABLE" }` filter and `{ $set: { status: "BORROWED" } }` update.
- **Observed Result**:
  - Exactly 1 thread successfully claims the copy and receives `HTTP 200 OK`.
  - Exactly 19 threads receive `HTTP 409 Conflict: All physical copies of '<Title>' are currently checked out. You can reserve this book.`
  - Database verification proves active loans count $= 1$, available copies $= 0$. Zero double-borrowing occurs.

---

## 4. Failure Scenario 4: Network Retries & Duplicate Transaction Defense

### Failure Injected
Client experiences a network timeout after submitting a fine payment or book return, leading the browser or API client to retry the identical request.

### Observed Behavior
1. **Idempotent Payments**: Client submits UUID idempotency key `k1`. On retry, the system retrieves the existing payment document matching `k1` and returns the identical receipt with `isDuplicateReplay=true`. No duplicate financial charge is recorded.
2. **Idempotent Returns**: Returning an already-returned physical copy returns `status="ALREADY_RETURNED"`. No duplicate overdue fine records are created, and inventory counts do not falsely increment.

---

## 5. Observed Bottlenecks, Reliability & Design Trade-Offs

| Architectural Decision | Trade-Off Made | Observed Bottleneck / Limitation | Mitigation in Production |
|---|---|---|---|
| **MongoDB Atomic findAndModify** | Strong document-level consistency without distributed two-phase commit overhead | Single document write lock contention under extreme hotspot borrowing | Queue-based checkout processing (e.g. RabbitMQ/Kafka FIFO topic) |
| **In-Memory Prefix Trie** | Sub-millisecond autocomplete suggestions with zero database I/O | Consumes JVM heap memory; requires index rebuild on server boot | Warm-up loading during `DataSeeder` initialization; offload to Redis Search if memory constrained |
| **Snowflake ID Generator** | High-throughput 64-bit ID generation without central database autoincrement lock | Susceptible to system clock backward drift (NTP adjustment) | Drift detection loop: wait until system clock catches up before issuing next ID |
