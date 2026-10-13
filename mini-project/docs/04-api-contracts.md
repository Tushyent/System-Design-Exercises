# Phase 1: REST API Contracts
## Scalable Library Management System

All endpoints follow standard REST conventions. Errors return a unified structure:
```json
{
  "timestamp": "2026-10-09T22:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Copy COPY-1002 is currently unavailable",
  "path": "/api/circulation/borrow"
}
```

---

### 1. Authentication Endpoints (`/api/auth`)
- **POST `/api/auth/register`**
  - Body: `{ "name": "John Doe", "email": "john@ssn.edu.in", "password": "SecurePassword123", "role": "MEMBER" }`
  - Response (201 Created): `{ "memberId": "MEM-1001", "name": "John Doe", "email": "john@ssn.edu.in", "token": "jwt..." }`
- **POST `/api/auth/login`**
  - Body: `{ "email": "john@ssn.edu.in", "password": "SecurePassword123" }`
  - Response (200 OK): `{ "token": "eyJ...", "member": { "id": "...", "role": "MEMBER" } }`

---

### 2. Catalogue & Search Endpoints (`/api/books`)
- **GET `/api/books?page=0&size=10&category=...`**
  - Response (200 OK): Paginated book list.
- **GET `/api/books/search?q=algorithms&mode=optimized`**
  - Supports query parameter `mode=baseline` (unindexed Mongo regex) or `mode=optimized` (Redis cache + indexed search).
- **GET `/api/books/{id}`**
  - Response: Full book metadata, external ratings, and copy availability breakdown.
- **POST `/api/books`** *(Librarian / Admin)*
  - Body: `{ "title": "Designing Data-Intensive Applications", "isbn": "9781449373320", "authors": ["Martin Kleppmann"], "categories": ["Distributed Systems"], "initialCopies": 3 }`

---

### 3. Circulation Endpoints (`/api/circulation`)
- **POST `/api/circulation/borrow`**
  - Body: `{ "bookId": "BOOK-101", "memberId": "MEM-1001" }`
  - Response (200 OK): `{ "loanId": "LOAN-5501", "copyId": "COPY-301", "dueDate": "2026-10-23T22:30:00Z" }`
  - Status 409 if no available copies.
- **POST `/api/circulation/return`**
  - Body: `{ "copyId": "COPY-301" }`
  - Response (200 OK): `{ "loanId": "LOAN-5501", "status": "RETURNED", "overdueDays": 0, "fineAmount": 0.0, "assignedToReservation": null }`
- **GET `/api/circulation/my-loans`** *(Authenticated Member)*
  - Response: List of active loans and due dates for requesting member.

---

### 4. Reservation Endpoints (`/api/reservations`)
- **POST `/api/reservations`**
  - Body: `{ "bookId": "BOOK-101", "memberId": "MEM-1001" }`
  - Response (201 Created): `{ "reservationId": "RES-801", "queuePosition": 1, "status": "PENDING" }`
- **GET `/api/reservations/my-reservations`**
  - Response: Active reservations with real-time queue position.
- **DELETE `/api/reservations/{reservationId}`**
  - Cancel a pending reservation.

---

### 5. Fines & Simulated Payments (`/api/fines`, `/api/payments`)
- **GET `/api/fines/my-fines`**
  - Response: Unpaid fine records, accrued overdue days, total balance.
- **POST `/api/payments/pay`**
  - Body: `{ "fineId": "FINE-201", "amount": 15.00, "idempotencyKey": "pay-uuid-771" }`
  - Response (200 OK): `{ "paymentId": "PAY-901", "status": "SUCCESS", "transactionRef": "SIM-TX-4402" }`
  - Duplicate requests with identical `idempotencyKey` return identical receipt without re-charging.

---

### 6. Autocomplete (`/api/autocomplete`)
- **GET `/api/autocomplete?prefix=des&limit=8`**
  - Response (200 OK):
  ```json
  {
    "query": "des",
    "latencyMs": 1.4,
    "suggestions": [
      { "text": "Designing Data-Intensive Applications", "type": "TITLE", "id": "b101" },
      { "text": "Design Patterns", "type": "TITLE", "id": "b102" }
    ]
  }
  ```

---

### 7. URL Shortener (`/s`, `/api/urls`)
- **POST `/api/urls/shorten`**
  - Body: `{ "targetUrl": "/books/6705f102", "resourceType": "BOOK", "resourceId": "b101" }`
  - Response (200 OK): `{ "shortCode": "bK7x9", "shortUrl": "/s/bK7x9" }`
- **GET `/s/{shortCode}`**
  - HTTP 302 Redirect to target URL (or 200 with redirect payload for SPA clients).

---

### 8. Web Crawler Management (`/api/crawler`)
- **POST `/api/crawler/trigger`** *(Admin)*
  - Body: `{ "sourceUrl": "http://localhost:8080/mock-external-library/books" }`
  - Response (202 Accepted): `{ "jobId": "CRAWL-101", "status": "STARTED" }`
- **GET `/api/crawler/jobs`**
  - Returns history of crawl executions, records fetched, parsed, deduplicated, and imported.

---

### 9. Consistent Hashing & Sharding Diagnostics
- **GET `/api/consistenthash/ring`**
  - Returns current hash ring state, virtual nodes count, node list.
- **POST `/api/consistenthash/simulate`**
  - Body: `{ "keysCount": 1000, "nodes": ["node1", "node2", "node3"], "vnodes": 100 }`
  - Returns load distribution, variance, remapping percentage when adding/removing nodes.
- **GET `/api/sharding/stats`**
  - Returns data distribution count across shards and collection sharding states.
- **GET `/api/metrics/summary`**
  - Live system design metrics: cache hit ratio, rate limiter rejections, ID generator throughput, autocomplete latency.
