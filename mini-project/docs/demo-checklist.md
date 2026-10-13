# Athena LMS: Demonstration Checklist & Step-by-Step Script

**Coursework**: SSN CSE V Semester UCS3513 System Design Laboratory  
**Project**: Scalable Library Management System (Problem Statement 3)  

---

## 1. Environment & Service Verification

- [x] **MongoDB 8.0**: Active and listening on `localhost:27017`.
- [x] **Spring Boot 3.3.0 Backend**: Active and listening on `http://localhost:8080`.
- [x] **React 18 / TypeScript Frontend**: Bundled in `frontend/dist/` and runnable via `npm run dev` at `http://localhost:5173`.
- [x] **OpenAPI / Swagger**: Accessible at `http://localhost:8080/swagger-ui.html`.

---

## 2. Step-by-Step Demonstration Script

### Step 1: Member Authentication & Token Generation
1. Navigate to `http://localhost:5173`.
2. Click **Sign In** in the top-right header.
3. Click the quick demo button **"Demo Member"** (logs in as `member@ssn.edu.in` / `member123`).
4. Notice the top right displays the member's name and role (`MEMBER`), and the JWT is stored in session.

### Step 2: Trie Autocomplete (Component #8)
1. In the search box, begin typing `"dist"`.
2. Observe instantaneous suggestions appearing in $< 1$ ms with ranked categories and titles:
   - *Distributed Systems: Principles and Paradigms*
   - *Distributed Systems* (Category)
3. Notice the latency badge displays `< 1ms` powered by the in-memory Prefix Trie.
4. Click on the book to open the detail view.

### Step 3: Copy Availability & URL Shortener (Component #6)
1. On the Book Detail modal:
   - Notice the physical copy barcodes (e.g. `BAR-62976-1`, `BAR-62976-2`).
   - Notice the current copy status (`AVAILABLE`).
2. Click **Copy Link** next to the Short URL (`/s/8kY7...`).
3. Open a new browser tab and navigate to `http://localhost:8080/s/<code-copied>`.
4. Observe HTTP 302 redirection directly back to the book resource!

### Step 4: Atomic Borrowing & Cache Invalidation (Components #4 & #5)
1. Click **"Borrow Book"** on an available copy.
2. The loan is created atomically using a 64-bit Snowflake ID (`LOAN-xxxxx`).
3. The copy status dynamically changes to `BORROWED`.
4. The Redis cache for this book is immediately invalidated.

### Step 5: Duplicate Borrow Concurrency Defense
1. Open an incognito browser window and sign in as `tushyent@ssn.edu.in`.
2. Try borrowing a book whose available copies have been exhausted.
3. Notice the system cleanly returns `HTTP 409 Conflict: All physical copies are currently checked out. You can reserve this book.`
4. Zero double-borrowing occurs.

### Step 6: Book Return & Overdue Fine Accrual
1. Switch to the **My Loans** tab (`/loans`).
2. Observe the active loan with its due date.
3. Click **"Return Copy"**.
4. The copy status updates to `RETURNED`, and available inventory restores.
5. If the due date was in the past, an overdue fine is automatically calculated ($1.50/day).

### Step 7: Idempotent Fine Payments
1. Navigate to the **Fines & Payments** tab (`/fines`).
2. If unpaid fines exist, click **"Pay Fine"**.
3. Notice the client generates a unique UUID Idempotency Key.
4. Click **"Confirm Payment"**.
5. Replay the submission: notice the system returns identical payment receipt with `isDuplicateReplay=true` without double debiting!

### Step 8: Web Crawler Ingestion (Component #7)
1. Log in as Admin (`admin@ssn.edu.in` / `admin123`).
2. Navigate to the **Admin** tab (`/admin`).
3. Under **Simulated External Web Crawler**, enter the mock URL: `http://localhost:8080/mock-external-library/books`.
4. Click **"Trigger Ingestion Crawl"**.
5. Observe the background worker fetch 7 external books, import new titles, deduplicate existing ISBNs, and record throughput (56.9 books/sec)!

### Step 9: System Design Dashboard & Consistent Hashing (Components #1, #2, #3)
1. Switch to the **System Design** tab (`/system-design`).
2. View live telemetry cards:
   - **Rate Limiter**: Rejection stats and token bucket overhead.
   - **Key-Value Store**: Redis cache hit ratio.
   - **Snowflake ID**: Total unique IDs generated and 0 collisions.
   - **Shard Distribution**: Member and Book distribution across partitions.
3. Scroll to the **Consistent Hashing Simulator**:
   - Run the simulation with 2,000 keys and 160 virtual nodes.
   - Observe the chart showing **28.3%** keys remapped for Consistent Hashing vs **75.1%** for Modulo Hashing!
4. Scroll to the **Autocomplete Precision@k Evaluator**:
   - Click "Run Benchmark": view mean latency ($< 1$ms) and top-$k$ precision scores!
