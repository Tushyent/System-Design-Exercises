# Phase 2: Data Model and Database Design
## Scalable Library Management System

### 1. Document Entities and Schemas

#### 1.1 Member (`members`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `memberId` (String): Indexed identifier (e.g. `MEM-100234`)
- `name` (String): Full name of the member
- `email` (String): Unique email index
- `passwordHash` (String): BCrypt hashed password
- `role` (Enum): `MEMBER`, `LIBRARIAN`, `ADMIN`
- `status` (Enum): `ACTIVE`, `SUSPENDED`, `INACTIVE`
- `maxBorrowLimit` (int): Maximum concurrent borrowed books (default: 5)
- `createdAt` (Instant), `updatedAt` (Instant)
- **Indexes**: `{"email": 1}` (Unique), `{"memberId": 1}` (Unique)
- **Shard Key**: `{"memberId": "hashed"}`

#### 1.2 Book (`books`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `isbn` (String): Standard International Book Number
- `title` (String): Full book title
- `authors` (List<String>): List of authors
- `categories` (List<String>): e.g. ["Computer Science", "Distributed Systems"]
- `publisher` (String): Publishing house
- `publicationYear` (int): Year of release
- `description` (String): Book synopsis
- `externalRating` (Double): Aggregated external rating (e.g. 4.8 / 5.0) enriched by crawler
- `externalSource` (String): Source identifier (e.g. "MockLibraryApi")
- `totalCopies` (int): Total count of copies in library
- `availableCopies` (int): Authoritative cached count of available copies
- `createdAt` (Instant), `updatedAt` (Instant)
- **Indexes**: `{"isbn": 1}`, `{"title": "text", "authors": "text"}`, `{"categories": 1}`
- **Shard Key**: `{"_id": "hashed"}`

#### 1.3 BookCopy (`book_copies`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `bookId` (String): Reference to `Book.id`
- `barcode` (String): Unique physical barcode sticker on book
- `status` (Enum): `AVAILABLE`, `BORROWED`, `RESERVED_FOR_PICKUP`, `MAINTENANCE`, `LOST`
- `currentLoanId` (String, nullable): Reference to active loan
- `currentHolderId` (String, nullable): Reference to member currently possessing or reserving copy
- `pickupDeadline` (Instant, nullable): Window expiration if reserved
- **Indexes**: `{"bookId": 1, "status": 1}`, `{"barcode": 1}` (Unique)
- **Concurrency Control**: Atomic MongoDB `findAndModify` checking `{_id: copyId, status: "AVAILABLE"}` with `$set: {status: "BORROWED"}`.

#### 1.4 Loan (`loans`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `loanId` (String): Friendly identifier
- `memberId` (String): Reference to `Member.id`
- `bookId` (String): Reference to `Book.id`
- `copyId` (String): Reference to `BookCopy.id`
- `borrowedAt` (Instant): Borrowing timestamp
- `dueDate` (Instant): Return deadline (default: 14 days)
- `returnedAt` (Instant, nullable): Timestamp of return
- `status` (Enum): `ACTIVE`, `RETURNED`, `OVERDUE`
- **Indexes**: `{"memberId": 1, "status": 1}`, `{"copyId": 1, "status": 1}`

#### 1.5 Reservation (`reservations`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `bookId` (String): Reference to `Book.id`
- `memberId` (String): Reference to `Member.id`
- `queuePosition` (int): Monotonically increasing queue position
- `status` (Enum): `PENDING`, `READY_FOR_PICKUP`, `FULFILLED`, `CANCELLED`, `EXPIRED`
- `createdAt` (Instant)
- `allocatedCopyId` (String, nullable): Assigned copy upon return
- **Indexes**: `{"bookId": 1, "status": 1, "queuePosition": 1}`, `{"memberId": 1, "bookId": 1}`

#### 1.6 Fine (`fines`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `loanId` (String): Associated loan
- `memberId` (String): Borrower
- `overdueDays` (int): Number of days overdue
- `ratePerDay` (double): Daily penalty rate ($1.50)
- `amount` (double): Total fine amount (capped at $50.00)
- `status` (Enum): `UNPAID`, `PAID`
- `createdAt` (Instant)
- **Indexes**: `{"memberId": 1, "status": 1}`

#### 1.7 Payment (`payments`)
- `id` (String / 64-bit Snowflake ID): Primary key
- `fineId` (String): Target fine
- `memberId` (String): Payer
- `amount` (double): Amount paid
- `idempotencyKey` (String): Unique client key to prevent duplicate payment execution
- `status` (Enum): `SUCCESS`, `FAILED`
- `transactionRef` (String): Simulated gateway reference
- `createdAt` (Instant)
- **Indexes**: `{"idempotencyKey": 1}` (Unique)

#### 1.8 ShortUrl (`short_urls`)
- `id` (String): Primary key
- `shortCode` (String): Base62 generated code (e.g. `bK7x9`)
- `targetUrl` (String): Full original URL (e.g. `/books/6705f...`)
- `resourceType` (Enum): `BOOK`, `RESERVATION`
- `resourceId` (String): Target entity ID
- `clickCount` (long): Analytic counter
- `createdAt` (Instant)
- **Indexes**: `{"shortCode": 1}` (Unique)

#### 1.9 CrawlerJob & ImportedBook (`crawler_jobs`, `imported_books`)
- `jobId` (String): Crawler job execution identifier
- `sourceUrl` (String): Target external catalog endpoint
- `status` (Enum): `RUNNING`, `COMPLETED`, `FAILED`
- `recordsFetched` (int), `recordsImported` (int), `recordsDeduplicated` (int), `recordsFailed` (int)
- `startedAt` (Instant), `finishedAt` (Instant)
- `dedupFingerprint` (String): MD5(ISBN + Title + Author)
