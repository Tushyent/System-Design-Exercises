# Job Portal — Automated Job Discovery Crawler + Redis Token Bucket

Spring Boot REST API that discovers job postings from job websites (BFS crawl
over a URL queue with a visited-URL set) and protects the crawl endpoint with
a per-client Redis token-bucket rate limiter.

## Architecture

```
                +------------------+     POST /api/crawl {seedUrl}     +--------------------------+
                | Client / Postman | --------------------------------> | Spring Boot REST API     |
                | X-Client-Id hdr  | <-------------------------------- | JobCrawlerController     |
                +--------+---------+      crawl JSON / HTTP 429        +-----+----------+---------+
                         |                                                     |          |
        seed/queue/      |                                                     | check    | crawl
        visited reads    |                                                     v          v
                         |                                            +----------+   +--------------+
                         +--------------------------------------------| Token    |   | Job crawler  |
                                                                      | Bucket   |   | (BFS, max 10)|
                                                                      +---+------+   +------+-------+
                                                                          |                 |
                    Redis (centralised state)  <----------------------------+-----------+-----+
                    - ratelimit:{client} hash {tokens, ts}  (bucket per client)         |
                    - jobcrawl:queue list (RPUSH/LPOP FIFO)                             | fetch/parse
                    - jobcrawl:visited set (SADD/SISMEMBER dedup)                       v
                                                                      +----------------------------------+
                                                                      | External job website (/mock/jobs)|
                                                                      | hub -> postings + categories     |
                                                                      +----------------------------------+
```

Request flow: Postman → `POST /api/crawl` → client resolved
(`X-Client-Id` → `clientId` param → IP) → Lua-script token acquire in Redis
(allowed → crawl; empty → **429 + Retry-After**) → BFS: seed → fetch page →
extract job hyperlinks → enqueue new URLs → visited set skips duplicates.

## Quick start

```powershell
docker compose up -d redis          # Redis on localhost:6379
mvn spring-boot:run                 # app on localhost:8080
python scripts/demo_requests.py     # normal + burst + refill demo
# Swagger: http://localhost:8080/swagger-ui.html
# Dashboard (frontend): http://localhost:8080/
# Postman: import postman/JobPortalCrawler.postman_collection.json
```

Run the Postman collection folders in order: Seeds → (a) Normal crawl →
(a) Duplicate demo → (b) Burst 8x → (b) Refill + isolation → Reset.

## REST API

```
POST   /api/crawl                  { "seedUrl": "http://..." }  (X-Client-Id header)
GET    /api/queue?limit=50         FIFO frontier + remainder
GET    /api/visited                visited-URL set
GET    /api/bucket/{clientId}      bucket view (no token consumed)
DELETE /api/bucket/{clientId}      reset bucket to full
POST   /api/reset                  clear queue + visited
GET    /api/mock-urls              seed URLs of the mock job site
GET    /mock/jobs...               mock external job website (HTML)
```

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `ratelimit.capacity` | `5` | bucket size (burst) |
| `ratelimit.refill-per-second` | `1` | sustained rate |
| `crawler.max-pages-per-request` | `10` | BFS page cap per request |
| `ratelimit.store` | `redis` | `redis` (Lua) or `memory` |

## Tests

```powershell
mvn test   # 6 tests, no Redis needed (in-memory fallback + stub web)
```

## Analysis (question c)

See [docs/analysis.md](docs/analysis.md) for the full write-up: queue
management, visited-set dedup, burst control, Redis centralisation, and the
burst-then-refill behaviour.
