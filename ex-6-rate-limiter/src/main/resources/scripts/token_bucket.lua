-- Token Bucket rate limiter - atomic acquire of a single token.
--
-- KEYS[1] : bucket key (e.g. "ratelimit:client-1")
-- ARGV[1] : bucket capacity (max tokens)
-- ARGV[2] : refill rate (tokens per second)
-- ARGV[3] : current time (epoch millis)
-- ARGV[4] : TTL seconds for the bucket key (stale-client cleanup)
--
-- State is a Redis hash: { tokens: double, ts: epoch millis }
--
-- Return value (a single 64-bit integer):
--   >= 0  -> ALLOWED. magnitude / 1000.0 = remaining tokens after consumption
--   <  0  -> REJECTED. (-value - 1) / 1000.0 = remaining tokens (always < 1)
--
-- Because the whole read/refill/consume/write happens inside the script, it is
-- atomic: Redis serialises script execution, so concurrent requests from any
-- number of application instances can never overspend a bucket.
--
local key      = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate     = tonumber(ARGV[2])
local now      = tonumber(ARGV[3])
local ttl      = tonumber(ARGV[4])

local fields = redis.call('HMGET', key, 'tokens', 'ts')
local tokens
local ts

if fields[1] then
    tokens = tonumber(fields[1])
    ts     = tonumber(fields[2])
else
    tokens = capacity   -- first visit: start with a full bucket
    ts     = now
end

local elapsed = (now - ts) / 1000.0
local refilled = tokens + elapsed * rate
if refilled > capacity then refilled = capacity end

local allowed = 0
if refilled >= 1 then
    refilled = refilled - 1
    allowed = 1
end

redis.call('HMSET', key, 'tokens', refilled, 'ts', now)
redis.call('EXPIRE', key, ttl)

local remainingThousandths = math.floor(refilled * 1000)
if allowed == 1 then
    return remainingThousandths
end
-- Guaranteed negative even when the bucket is completely empty.
return -remainingThousandths - 1
