# Token Bucket API Rate Limiter with Redis — Lab Exercise 6

**UCS3513 System Design Laboratory** — Sri Sivasubramaniya Nadar College of Engineering

A Spring Boot REST API that limits the number of requests each client can send
using the **Token Bucket** algorithm. Per-client token counts and last-refill
timestamps are stored in **Redis**, and token updates are performed atomically
with a **Redis Lua script** so the limit holds across multiple application
instances.

## What the assignment requires (and where it is)

| Task | Where |
|------|-------|
| Develop a REST API using Spring Boot | `src/main/java/com/ratelimiter/` |
| Configure Redis with the app | `config/RedisConfig.java`, `application.properties` |
| Identify clients by client ID or IP address | `RateLimiterService.resolveClientId()` |
| Token bucket with configurable capacity & refill rate | `ratelimit.capacity`, `ratelimit.refill-per-second` |
| Store token count & last refill time in Redis | hash `ratelimit:<clientId>` = `{tokens, ts}` |
| HTTP 200 when tokens available, HTTP 429 otherwise | `GlobalExceptionHandler` (+ `Retry-After`) |
| Generate normal & burst requests, observe behaviour | `scripts/load_test.py` |
| Test concurrent requests with atomic Lua updates | `src/main/resources/scripts/token_bucket.lua` |
| Report + analysis + evidence screenshots | `scripts/gen_report.py`, `docs/analysis.md`, `docs/SCREENSHOT_PLAN.md` |

## Quick start

```powershell
# Windows — starts Redis (docker), builds, runs, runs all demos, writes SD_A6.docx
.\run_lab.ps1

# Linux/macOS
./run_lab.sh
```

Manual steps:

```bash
docker compose up -d redis        # 1. Redis on localhost:6379
mvn spring-boot:run               # 2. run the app on localhost:8080
python scripts/load_test.py normal --client demo      # 3. normal traffic
python scripts/load_test.py burst  --client demo      #    burst traffic
python scripts/load_test.py concurrent --client demo  #    concurrent test
# Swagger: http://localhost:8080/swagger-ui.html
```

## REST API

```
GET    /api/hello?clientId=demo    protected resource (200 while tokens remain, 429 otherwise)
GET    /api/bucket/{clientId}      read-only view of a client's bucket
DELETE /api/bucket/{clientId}      reset a client's bucket
POST   /api/test/concurrent        { clientId, threads, requestsPerThread, capacity, refillPerSecond }
```

Client identity priority: `X-Client-Id` header → `clientId` query parameter →
remote IP address.

## How the token bucket works

Every request executes one atomic Lua script (`scripts/token_bucket.lua`):

1. Read `{tokens, ts}` for the client (initialise full on first visit).
2. Refill: `tokens = min(capacity, tokens + (now - ts) × refillRate)`.
3. If `tokens >= 1`: consume one token, return allowed; else return rejected.
4. Write `{tokens, ts = now}` back with a TTL for stale-client cleanup.

Because Redis runs the script atomically, concurrent requests (threads and
instances) can never double-spend a token.

## Configuration (`application.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `ratelimit.capacity` | `10` | Max tokens per client bucket (burst size) |
| `ratelimit.refill-per-second` | `5` | Tokens added per second (sustained rate) |
| `ratelimit.prefix` | `ratelimit` | Redis key prefix |
| `ratelimit.ttl-seconds` | `3600` | Bucket key TTL |
| `ratelimit.store` | `redis` | `redis` (Lua) or `memory` (test double) |
| `spring.data.redis.host/port` | `localhost:6379` | Redis connection |

## Observed results (capacity 10, refill 5/s)

| Scenario | Allowed | Rejected |
|----------|--------:|---------:|
| Normal: 10 requests @ 250 ms | 10 | 0 |
| Burst: 50 simultaneous | 10 | 40 |
| Concurrent: 20 threads × 20 req (400) | 10 | 390 |

Full analysis (the 5 written questions) is in [`docs/analysis.md`](docs/analysis.md).

## Report (`SD_A6.docx`)

`python scripts/gen_report.py` builds the whole lab report: cover page, Aim,
Question, Procedure, architecture diagram, design (algorithm, config, client
identification, Redis state, Lua script), output results with charts + tables,
the 5 analysis answers, API/command reference, tech stack, test-case table,
conclusion and learning outcomes. Everything is generated from the observed data
in `reports/`, so the numbers always match a real run.

**Figure slots.** The report has fixed image slots under `assets/images/`:

- Charts (auto-generated): `fig_architecture`, `fig_normal`, `fig_burst`,
  `fig_drain`, `fig_concurrent`.
- Evidence screenshots (captured, then embedded): `fig_swagger`,
  `fig_hello_200`, `fig_429_error`, `fig_startup`, `fig_mvn_test`,
  `fig_concurrent_json`, `fig_429_headers`, `fig_hgetall`.

A slot is embedded only when its file exists, so the report is complete with
just the charts and becomes richer as screenshots are added.

### Capturing the evidence screenshots

`docs/SCREENSHOT_PLAN.md` lists every slot, the exact URL/command that produces
it, and **ready-to-paste Gemini prompts** you can give to an agent that uses
AgentBrowser / Playwright. Quick start:

```powershell
docker compose up -d redis
mvn spring-boot:run                 # keep this shell open (app on :8080)

# Browser slots (Playwright) — needs: pip install playwright
python -m playwright install chromium
python scripts/capture_screenshots.py --mode browser

# Terminal slots (startup log, mvn test, curl -i 429, redis-cli HGETALL,
# concurrent JSON): run the commands in docs/SCREENSHOT_PLAN.md §4 / §5
# or hand the master prompt (§3) to your Gemini/agentbrowser agent.

python scripts/gen_report.py         # re-embed the new screenshots
```

## Tests

```bash
mvn test        # 33 tests: 26 unit/API tests always run; the 7 Redis-backed
                # integration tests (Lua path) run when Redis is reachable
```

## Clean up

```bash
docker compose down
```
