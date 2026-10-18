# Role-Based Permission & Security Matrix
## Vidya LMS — Scalable Library Management System (UCS3513)

**Document Version:** 2.0 (Post-Audit Hardening)  
**Security Framework:** Spring Security 6 / Java JWT / Stateless Token Verification  
**Evaluation Date:** October 2026  

---

## 1. Overview of System Roles

The Vidya LMS implements Role-Based Access Control (RBAC) following the principle of least privilege:

1. **Anonymous (Public Visitor):** Unauthenticated guest user. Can browse the book catalogue, execute autocomplete prefix lookups, perform catalogue searches, view book details, and resolve short URLs.
2. **Student / Faculty Member (`ROLE_MEMBER`):** Registered library member. Inherits all anonymous privileges plus the ability to borrow copies, renew loans, place reservations, cancel owned reservations, view personal borrowing history, review fine balances, and pay fines.
3. **Librarian (`ROLE_LIBRARIAN`):** Operational staff member. Inherits all member privileges plus the authority to execute circulation operations on behalf of any member, manage physical copy inventory, process walk-in returns, and view system analytics.
4. **Administrator (`ROLE_ADMIN`):** Full system administrator. Inherits all librarian privileges plus the ability to create new titles, trigger asynchronous web crawler ingestion, clear and invalidate caches, adjust system rate limiter settings, and monitor low-level infrastructure health.

---

## 2. API Endpoint Role & Permission Matrix

| Endpoint Path | HTTP Method | Permitted Roles | Spring Security Rule | Authentication Required | Ownership Enforced | HTTP Failure Code |
|---|:---:|---|---|:---:|:---:|:---:|
| `/api/auth/register` | `POST` | Public / All | `permitAll()` | No | N/A | `400 Bad Request` |
| `/api/auth/login` | `POST` | Public / All | `permitAll()` | No | N/A | `401 Unauthorized` |
| `/api/auth/me` | `GET` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Yes (JWT Principal) | `401 Unauthorized` |
| `/api/books` | `GET` | Public / All | `permitAll()` | No | N/A | `500 Server Error` |
| `/api/books/{id}` | `GET` | Public / All | `permitAll()` | No | N/A | `404 Not Found` |
| `/api/books/search` | `GET` | Public / All | `permitAll()` | No | N/A | `400 Bad Request` |
| `/api/autocomplete` | `GET` | Public / All | `permitAll()` | No | N/A | `400 Bad Request` |
| `/s/{code}` | `GET` | Public / All | `permitAll()` | No | N/A | `404 Not Found` |
| `/api/books` | `POST` | `LIBRARIAN`, `ADMIN` | `hasAnyRole('LIBRARIAN', 'ADMIN')` | Yes | No | `403 Forbidden` |
| `/api/books/{id}/copies` | `POST` | `LIBRARIAN`, `ADMIN` | `hasAnyRole('LIBRARIAN', 'ADMIN')` | Yes | No | `403 Forbidden` |
| `/api/circulation/borrow` | `POST` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Staff Override) | `403 / 409 Conflict` |
| `/api/circulation/renew` | `POST` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Staff Override) | `403 / 409 Conflict` |
| `/api/circulation/return` | `POST` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Staff Override) | `409 Conflict` |
| `/api/circulation/my-loans` | `GET` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Principal ID) | `401 Unauthorized` |
| `/api/circulation/my-history` | `GET` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Principal ID) | `401 Unauthorized` |
| `/api/reservations` | `POST` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Staff Override) | `409 Conflict` |
| `/api/reservations/my-reservations` | `GET` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Principal ID) | `401 Unauthorized` |
| `/api/reservations/{id}` | `DELETE` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Staff Override) | `403 Forbidden` |
| `/api/fines/my-fines` | `GET` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Principal ID) | `401 Unauthorized` |
| `/api/payments/pay` | `POST` | `MEMBER`, `LIBRARIAN`, `ADMIN` | `authenticated()` | Yes | Self (Fine Record) | `409 Conflict` |
| `/api/crawler/crawl` | `POST` | `ADMIN` | `hasRole('ADMIN')` | Yes | N/A | `403 Forbidden` |
| `/api/system/metrics` | `GET` | Public / Staff | `permitAll()` | No | N/A | `500 Server Error` |

---

## 3. Ownership Verification & Security Safeguards

### 3.1 Self-Service Ownership Enforcement
When a member invokes a protected endpoint such as `/api/circulation/renew` or `/api/reservations/{id}`, the backend enforces ownership:
- The calling user's unique `memberId` is extracted directly from the verified `UserPrincipal` in the Spring Security Context (derived from the cryptographically validated JWT).
- The service verifies that the targeted resource (Loan, Reservation, or Fine) belongs to the authenticated `memberId`.
- If an ordinary `MEMBER` attempts to renew or cancel another user's loan or reservation, the system throws a `ConflictException` with HTTP 403/409, rejecting the operation.

### 3.2 Staff Administrative Delegation
Users with `ROLE_LIBRARIAN` or `ROLE_ADMIN` bypass the self-service ownership restriction. If a staff member submits a request with an explicit `memberId` in the request body, the operation is executed on behalf of that member (e.g., walk-in book checkouts at the circulation desk).

### 3.3 Rate Limiting Enforcement
All incoming requests pass through the Redis Token Bucket filter before reaching controllers:
- In unauthenticated contexts, the client IP address (`X-Forwarded-For` or remote socket address) is used as the rate limit bucket key.
- In authenticated contexts, the user's `memberId` is used as the rate limit key.
- Requests exceeding the bucket limit are intercepted with `HTTP 429 Too Many Requests` and a `Retry-After: 60` response header.

---

## 4. Verification Evidence

The entire permission matrix was tested against unauthorized tokens, valid student credentials (`member@ssn.edu.in`), librarian credentials (`librarian@ssn.edu.in`), and administrative credentials (`admin@ssn.edu.in`). Zero privilege leakages or bypass vulnerabilities were observed.
