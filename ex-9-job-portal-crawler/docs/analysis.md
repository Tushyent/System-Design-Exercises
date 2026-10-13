# Demonstration & Analysis (question c)

## 1. How the URL queue manages discovered URLs

The frontier is a **FIFO queue** (`Queue<String>`; Redis list
`jobcrawl:queue` via `RPUSH`/`LPOP`, in-memory `ConcurrentLinkedQueue`
fallback). The seed is enqueued first; each crawl request dequeues from the
head, fetches up to **10 pages (BFS)**, and appends newly discovered job
hyperlinks at the tail. The response reports `discoveredUrls` (new this
request) and `queueRemaining` (still pending). Because it is FIFO, pages are
visited in breadth-first order: the hub first, then its direct postings, then
related postings — leftover URLs simply wait for the next `/api/crawl` call.

## 2. How the visited set prevents duplicate crawling

The visited set is a **`Set<String>`** (Redis set `jobcrawl:visited` via
`SADD`/`SISMEMBER`, in-memory `ConcurrentHashMap` set fallback) holding
canonicalised URLs. Before fetching, the crawler calls
`markVisitedIfAbsent`: first visit returns true (fetch it), repeats return
false (skip, counted in `duplicatesSkipped`). Re-crawling the same hub seed
therefore crawls 0 new pages: overlapping category links, back-links from
postings to the hub, and posting↔posting cycles are all suppressed, so the
crawl always terminates.

## 3. How the token bucket controls burst requests

Each client owns a bucket of **capacity 5, refill 1 token/second** stored in
Redis (`ratelimit:{client}` = `{tokens, ts}`). Every `/api/crawl` consumes one
token atomically (Lua script: refill → check → consume → write in one step).
A burst of 8 rapid requests admits exactly 5 (`200`) and rejects 3 with
**`429 Too Many Requests` + `Retry-After`**; after ~6 s idle, ~6 tokens refill
(capped at 5) and requests succeed again. Buckets are per-client, so one
client's burst never starves another.

## 4. How Redis supports centralised state management

Redis holds all three shared states — buckets (hash), queue (list), visited
(set) — so concurrent users/services and multiple app instances see one
consistent view: tokens cannot be double-spent (atomic Lua), queue order is
global (FIFO list), and duplicates are suppressed globally (shared set). Keys
carry TTLs where appropriate and the app degrades to in-memory structures if
Redis is down.

## 5. Behaviour under multiple rapid requests (Postman evidence)

| Step | Observation |
|---|---|
| Normal: 3 crawls 1 s apart | 3× `200`; queue shrinks, visited grows |
| Same seed again | `200`, `pagesCrawledThisRequest: 0`, duplicates skipped |
| Burst: 8 rapid (fresh client) | 5× `200`, then 3× `429` with `Retry-After` |
| Wait 6 s → retry | `200` again (refilled) |
| Fresh client during burst | `200` (per-client isolation) |
