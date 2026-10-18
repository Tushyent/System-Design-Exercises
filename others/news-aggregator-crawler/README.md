# News Aggregator — Automated News Discovery Crawler + Redis Token Bucket

Spring Boot REST API that discovers news articles from news websites (BFS crawl
over a URL queue with a visited-URL set) and protects the crawl endpoint with
a per-client Redis token-bucket rate limiter.

## Architecture

```
                +------------------+     POST /api/crawl {seedUrl}     +--------------------------+
                | Client / Postman | --------------------------------> | Spring Boot REST API     |
                | X-Client-Id hdr  | <-------------------------------- | NewsCrawlerController     |
                +--------+---------+      crawl JSON / HTTP 429        +-----+----------+---------+
                         |                                                     |          |
        seed/queue/      |                                                     | check    | crawl
        visited reads    |                                                     v          v
                         |                                            +----------+   +--------------+
                         +--------------------------------------------| Token    |   | News crawler |
                                                                      | Bucket   |   | (BFS, max 10)|
                                                                      +---+------+   +------+-------+
                                                                          |                 |
                    Redis (centralised state)  <----------------------------+-----------+-----+
                    - ratelimit:{client} hash {tokens, ts}  (bucket per client)         |
                    - newscrawl:queue list (RPUSH/LPOP FIFO)                             | fetch/parse
                    - newscrawl:visited set (SADD/SISMEMBER dedup)                       v
                                                                      +----------------------------------+
                                                                      | News website (/mock/news or real) |
                                                                      | front page -> stories + sections  |
                                                                      +----------------------------------+
```

Request flow: Postman → `POST /api/crawl` → client resolved
(`X-Client-Id` → `clientId` param → IP) → Lua-script token acquire in Redis
(allowed → crawl; empty → **429 + Retry-After**) → BFS: seed → fetch page →
extract article hyperlinks → enqueue new URLs → visited set skips duplicates.

## Quick start

```powershell
docker compose up -d redis          # Redis on localhost:6380
mvn spring-boot:run                 # app on localhost:8081
python scripts/demo_requests.py     # normal + burst + refill demo
# Swagger: http://localhost:8081/swagger-ui.html
# Dashboard (frontend): http://localhost:8081/
# Postman: import postman/NewsAggregatorCrawler.postman_collection.json
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
GET    /api/mock-urls              seed URLs of the mock news site
GET    /mock/news...               mock external news website (HTML)
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
