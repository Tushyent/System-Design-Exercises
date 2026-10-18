# Vidya LMS — Scalable Library Management System

**Mini Project 3 · UCS3513 System Design Laboratory**
Sri Sivasubramaniya Nadar College of Engineering · Team **Blastorz**

An end-to-end library management system that implements **all 8 system-design building blocks**
from the problem statement as real, working, load-tested features — a **Spring Boot** backend,
**MongoDB + Redis** data layer, and a **React (Vite + TypeScript)** frontend — shipped together
with benchmark harnesses, verification artifacts, UI screenshots, and a full project report.

---

## System Design Components

| # | Building Block | Where it lives in Vidya LMS | Headline result |
|---|----------------|-----------------------------|-----------------|
| 1 | **Rate Limiter** | Per-client Redis **Token Bucket** (atomic Lua script) guarding `/api/auth/*`, `/api/books/search`, `/api/autocomplete`, `/api/crawler/trigger` | 80% of abuse traffic rejected (40× HTTP 429 + 10× 401) |
| 2 | **Sharding** | MongoDB **hashed sharding** strategy for `books`, `members`, `loans`, `copies` (hashed keys, read-locality for loans) | Design + rationale in `docs/02-architecture.md`, `docs/03-data-model.md` |
| 3 | **Consistent Hashing** | VNode **hash ring** (100 virtual nodes per host, MD5) for key routing | On node change only **28.3%** of keys remap vs **75.1%** with naive modulo hashing |
| 4 | **Key-Value Store** | Redis **cache-aside** for book details, availability counts, hot search results, idempotency tokens | Instant invalidation on borrow/return; hit/miss + invalidation evidence in `results/` |
| 5 | **Unique ID Generator** | **Snowflake-style** 64-bit time-sortable IDs for loans, reservations, copy barcodes, payments | Collision-free under concurrency (0 duplicates); `MEM-xxxxxx`, `LOAN-…`, `FINE-…` |
| 6 | **URL Shortener** | Base62 short links for catalogue items & reservation confirmations (`/s/{code}`) | **5.10 ms** mean / **6.83 ms** P95 redirection latency |
| 7 | **Web Crawler** | Async BFS crawler enriching book metadata from simulated external sources with dedup fingerprinting | **90.9 books/s**, 7/7 deduplication correctness |
| 8 | **Autocomplete** | In-memory **prefix trie** + Redis prefix cache, Top-K ranking by popularity | **P@5 = 0.46 / R@5 = 0.80** at **2.32 ms** per suggestion |

Additional evidence: sustained-load throughput scaled from **313 → 507 req/s** across
concurrency tiers, catalogue search improved **38.3 ms → 35.3 ms** (1.08×) after tuning,
**10/10** backend tests and **14/14** end-to-end workflows verified.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17 · Spring Boot 3 · Spring Security (JWT, BCrypt) · Spring Data MongoDB · Spring Data Redis |
| Data | MongoDB 7 (sharded design) · Redis 7 (token bucket, cache, idempotency) |
| Frontend | React 18 · TypeScript · Vite · vanilla CSS design system (Deep Vidya Blue palette) |
| Tooling | Maven · Docker Compose · Python benchmark harness (`matplotlib`, `requests`) |

---

## Quick Start

**Prerequisites:** JDK 17+, Maven 3.9+, Node 18+, Docker Desktop (for Redis; MongoDB can be
the local service or a container).

```powershell
# Option A — one-click launcher (Windows)
.\run_system.ps1

# Option B — manual
docker start library-redis                    # or: docker compose up -d mongodb redis
cd backend ; mvn package -DskipTests ; java -jar target\scalable-library-system-1.0.0.jar
cd ..\frontend ; npm install ; npm run dev
```

| URL | Purpose |
|-----|---------|
| http://localhost:5173 | Vidya LMS web app |
| http://localhost:8080 | Backend REST API |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api/metrics/summary | Live runtime metrics |

### Demo Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@ssn.edu.in` | `admin123` |
| Librarian | `librarian@ssn.edu.in` | `librarian123` |
| Member | `member@ssn.edu.in` | `member123` |

> Seed data (accounts, 9-book catalogue, one overdue loan + fine) is inserted automatically on
> first backend start by `DataSeeder`.

---

## Project Layout

```
mini-project/
├── backend/       Spring Boot REST API — all 8 components (auth, catalogue, circulation,
│                  fines, autocomplete, crawler, shortener, id generator, rate limiter)
├── frontend/      React + Vite SPA — catalogue, search-as-you-type, member portal, admin ops
├── benchmarks/    Python harness — load tests, workflow verification, screenshot capture
├── results/       Evidence: raw benchmark JSON, matplotlib figures, screenshots (r01–r18)
├── docs/          Design & verification documents (requirements → architecture → test reports)
├── report/        Final report — Word editions (with/without rendered diagrams) + PDF
├── compose.yaml   Full-stack Docker Compose (MongoDB + Redis + backend + frontend)
├── run_system.ps1 One-click local launcher
└── .env.example   Configuration reference
```

### Documentation Index (selected)

| Document | Contents |
|----------|----------|
| `docs/00-requirements-and-audit.md` | Requirements extraction & workspace audit |
| `docs/01-component-mapping.md` | Mapping of the 8 building blocks to library features + alternatives considered |
| `docs/02-architecture.md` | System architecture, flows, consistency rules |
| `docs/03-data-model.md` | Collections, indexes, sharding keys |
| `docs/04-api-contracts.md` | REST API contracts |
| `docs/performance-report.md` | Quantitative benchmark results |
| `docs/test-report.md` | Unit & end-to-end test outcomes |
| `docs/final-verification-report.md` | End-to-end acceptance verification |
| `docs/failure-analysis.md` | Scalability & failure scenario analysis |
| `docs/role-permission-matrix.md` | Role-based security matrix |
| `docs/implementation-gap-analysis.md` | Gap analysis & resolution matrix |

…plus audit, checklist, and traceability documents — see the full list in `docs/`.

## Reports

| File | Description |
|------|-------------|
| `report/Vidya_LMS_MiniProject_Report.docx` | Full academic report (with prompt appendices) |
| `report/Vidya_LMS_MiniProject_Report_Diagrams.docx` | Same report with rendered architecture diagrams |
| `report/Vidya_LMS_MiniProject_Report.pdf` | PDF edition |

## Verification Artifacts

All quantitative claims above are reproducible with `benchmarks/run_benchmarks.py` (load
harness), `benchmarks/verify_workflows.py` (end-to-end workflow verifier) and
`benchmarks/capture_report_screenshots.py`, with outputs stored under `results/`
(`raw/` JSON, `figures/`, `verification/`, `screenshots/`).
