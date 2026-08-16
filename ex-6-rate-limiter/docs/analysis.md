# Analysis — Token Bucket API Rate Limiter Lab

Observed during the integration run (Spring Boot 3.3, Redis 7, capacity = 10
tokens, refill rate = 5 tokens/s, client identified by the `clientId` query
parameter / `X-Client-Id` header / IP address).

| Scenario | Requests | Allowed (HTTP 200) | Rejected (HTTP 429) | Notes |
|----------|---------:|-------------------:|--------------------:|-------|
| Normal (10 @ 250 ms interval) | 10 | 10 | 0 | Refill keeps the bucket topped up; `remaining` stayed at 9.0 |
| Burst (50 simultaneous) | 50 | 10 | 40 | Excess burst rejected instantly; capacity held at 10 |
| Drain (sequential) | 12 | 11 | 1 | 10 capacity + ~1 token refilled between requests |
| Concurrent (20 threads × 20 req = 400) | 400 | 10 | 390 | 70 ms total; no lost/overspent tokens |
| Bucket status after demos | — | 10.0 tokens | 0 s retry-after | Refill fully restored every bucket |

The 429 response carried `Retry-After: 1` and `X-RateLimit-Remaining: 0`; the
Redis state for a client is a hash `ratelimit:<clientId>` = `{tokens, ts}`
(verified with `redis-cli HGETALL`).

---

## 1. Role of the components

### Token Bucket
A Token Bucket is a rate-control structure that holds a number of "tokens". Each
incoming request must take one token from the bucket to be admitted; if the
bucket is empty the request is rejected. Tokens are added back continuously at a
steady refill rate, which means the algorithm permits a configurable **burst**
(up to the bucket size) while still enforcing a long-run average request rate.
In this lab the bucket state lives per client, so one aggressive client cannot
starve others.

### Bucket Capacity
The capacity is the maximum number of tokens a bucket can hold. It bounds the
size of an instantaneous burst: a client with a full 10-token bucket may fire 10
requests back-to-back before any are refused, even though the sustained rate is
only 5/s. Tokens can never exceed capacity — refill while idle simply fills to
capacity, never beyond. This is the "burst allowance" parameter
(`ratelimit.capacity=10`).

### Refill Rate
The refill rate is the number of tokens added to the bucket per second
(`ratelimit.refill-per-second=5`). It sets the **sustained** request ceiling
(here 5 requests per second) and is what makes the bucket self-recovering: after
a burst drains the bucket, tokens accumulate again so the client is gradually
re-admitted. Refill is computed lazily from wall-clock time (`elapsed × rate`)
at every request rather than with a background job, so the algorithm costs one
lazy calculation per request.

### Redis
Redis is the shared, low-latency key/value store that holds each client's bucket
state (`{tokens, ts}`) so that the rate limit is **centralized**. Every
application instance talks to the same Redis, so the limit is enforced globally
across the whole fleet rather than per JVM. Because Redis executes the Lua script
atomically, concurrent requests from many instances cannot double-spend tokens.
The per-key TTL (`ratelimit.ttl-seconds`) also cleans up stale clients
automatically.

## 2. How the Token Bucket algorithm allows or rejects requests

1. The client is identified (header > query parameter > IP).
2. A single atomic Lua script reads the bucket `{tokens, ts}` for that client (or initialises it full with `tokens = capacity` on first visit).
3. **Refill**: `tokens = min(capacity, tokens + (now - ts) / 1000 × refillRate)`.
4. **Decision**: if `tokens >= 1`, the request is allowed, one token is consumed (`tokens = tokens - 1`), and the new state plus `ts = now` are written back. If `tokens < 1`, the request is rejected and the state is written back unchanged.
5. The app answers **HTTP 200** in the allowed case and **HTTP 429 Too Many Requests** (with a `Retry-After` header) in the rejected case.

Because the whole read-refill-consume-write is one `EVAL`, the decision is
serialised by Redis — no two concurrent requests can both see the last token.

## 3. Why Redis is required with multiple application instances

Token state is **shared mutable state**: when N instances sit behind a load
balancer, each instance serves a slice of the traffic, but all instances must
agree on how many tokens remain for a client. If each instance kept its own
in-JVM bucket, the effective limit would multiply by N (instance-local buckets
each allow capacity tokens), defeating rate limiting — a client could send
`N × capacity` requests in a burst. Redis centralises the buckets, so a request
routed to any instance checks the same global bucket. The atomic Lua script
removes the read-modify-write race that plain GET-SET would introduce between
instances (and between threads within one instance), which is exactly the
concurrency guarantee demonstrated by the 400-request test admitting exactly 10.

## 4. Comparison of rate-limiting algorithms

| Aspect | Fixed Window | Sliding Window Log | Sliding Window Counter | Token Bucket |
|--------|--------------|--------------------|------------------------|--------------|
| Basis | Count in fixed time windows (e.g. 10 req per 1-min bucket) | Timestamp per request; count requests in the last W seconds | Two counters (current + previous window) weighted by overlap | Tokens refilled at a rate, up to a capacity |
| Memory | O(1) per client | O(requests) per client (one timestamp each) | O(1) per client (2 counters) | O(1) per client |
| Burst handling | A burst at window boundary can double the effective limit | Precise, but the trailing log grows with request volume | Approximate (no exact burst guarantee at boundary) | Exact burst allowance = capacity |
| Boundary behaviour | Stale: allows `2×limit` in the last instant of one window + first instant of the next | Exact at any instant (fully sliding) | Approximate at boundary | Smooth; no window boundary at all, purely time/rate based |
| Precision | Coarse (boundary spikes) | Very high | Good approximation | Exact steady rate + exact burst cap |
| Implementation cost | Trivial (INCR + TTL) | High storage; expiry per request | Moderate (2 counters + math) | Moderate (one atomic script) |
| Suitability | Simple dashboards | Strict per-instant limits, low QPS per client | High-QPS approximation of sliding window | API rate limiting with bursts (chosen for this lab) |

Token Bucket was chosen because it combines an exact sustained rate with an
explicit, controllable burst allowance, uses O(1) memory per client, and its
lazy refill makes it cheap and easy to keep correct in Redis via one Lua script.

## 5. Behaviour under normal and burst traffic

**Normal traffic (below the refill rate).** Ten requests were sent 250 ms apart
(4 requests/s against a 5 tokens/s refill). Every request returned HTTP 200 and
the bucket stayed at ~9.0 tokens: consumption was exactly matched by refill, so
the client never hit the ceiling and never drained the bucket. This is the
steady-state the limiter is designed to permit.

**Burst traffic (far above the refill rate).** Fifty requests were fired
simultaneously. Exactly the first 10 requests were admitted (HTTP 200) while the
remaining 40 received HTTP 429 with `Retry-After: 1`. This shows the two
governing parameters working together: the **capacity** capped the burst at 10,
and the **refill rate** will thereafter restore one token every 200 ms so a
sustained 5/s flow is re-admitted. The 400-request concurrency test (20 threads
× 20 requests) admitted exactly 10 and rejected 390 in ~70 ms with
`allowed + rejected == total`, proving that the Lua script keeps the count exact
under heavy simultaneous load — the correct behaviour for protecting a backend
from both a single-client flood and a coordinated multi-instance burst.

## Testability / design notes

- `TokenBucketMath` is the single source of truth for the refill/consume rules;
  the Redis Lua script mirrors it exactly. Unit tests validate the math and the
  in-memory store, and `RedisBucketStoreTest` re-validates the same rules through
  the real Lua path when Redis is reachable (33 tests, all green, none skipped
  in the lab run).
- The `BucketStore` interface lets tests run without Docker
  (`ratelimit.store=memory`) while production uses the atomic Lua store.
