"""Live verification against the running app (avoids shell quoting issues)."""
import json
import time
import urllib.request
import urllib.error

BASE = "http://localhost:8080"


def call(method, path, body=None, client=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if client:
        headers["X-Client-Id"] = client
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode()
            return r.status, dict(r.headers), json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, dict(e.headers), json.loads(raw)
        except Exception:
            return e.code, dict(e.headers), raw


print("== reset ==")
print(call("POST", "/api/reset"))
print(call("DELETE", "/api/bucket/demo-client"))
print(call("DELETE", "/api/bucket/burst-client"))

print("== (a) crawl hub seed ==")
s, h, b = call("POST", "/api/crawl", {"seedUrl": BASE + "/mock/jobs"}, "demo-client")
print("status", s, "remaining-hdr", h.get("X-RateLimit-Remaining"))
print("crawled", b["pagesCrawledThisRequest"], "max", b["maxPagesPerRequest"],
      "visited", b["visitedCount"], "queueRemaining", b["queueRemaining"],
      "dups", b["duplicatesSkipped"])
print("discovered sample", b["discoveredUrls"][:4])

print("== queue / visited ==")
print(call("GET", "/api/queue?limit=50")[2]["queueSize"], "queued")
v = call("GET", "/api/visited")[2]
print(v["visitedCount"], "visited;", sorted(v["visitedUrls"])[:3], "...")

print("== (a) same seed again (dedup) ==")
s, h, b = call("POST", "/api/crawl", {"seedUrl": BASE + "/mock/jobs"}, "demo-client")
print("status", s, "crawled", b["pagesCrawledThisRequest"], "dups", b["duplicatesSkipped"],
      "visited", b["visitedCount"], "queueRemaining", b["queueRemaining"])

print("== (b) burst 8 rapid (expect 5x200 then 429) ==")
for i in range(8):
    s, h, b = call("POST", "/api/crawl", {"seedUrl": BASE + "/mock/jobs"}, "burst-client")
    extra = ""
    if s == 200:
        extra = "crawled=%s queue=%s" % (b["pagesCrawledThisRequest"], b["queueRemaining"])
    else:
        extra = "retry-after=%s msg=%s" % (h.get("Retry-After"), b.get("message"))
    print("  #%d -> %s %s" % (i + 1, s, extra))

print("== (b) fresh client isolated -> 200 ==")
s, h, b = call("POST", "/api/crawl", {"seedUrl": BASE + "/mock/jobs"}, "fresh-client")
print("status", s)

print("== (b) wait 6s refill -> 200 again ==")
time.sleep(6)
s, h, b = call("POST", "/api/crawl",
               {"seedUrl": BASE + "/mock/jobs/category/engineering"}, "burst-client")
print("status", s)

print("== redis keys ==")
s, h, b = call("GET", "/api/bucket/burst-client")
print("bucket burst-client:", b)
