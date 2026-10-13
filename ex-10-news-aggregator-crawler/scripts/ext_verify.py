"""Crawl a real news website to verify external discovery."""
import json
import sys
import urllib.request
import urllib.error

BASE = "http://localhost:8081"
SEEDS = ["https://news.ycombinator.com/", "https://www.bbc.com/news"]
if len(sys.argv) > 1:
    SEEDS = sys.argv[1:]


def crawl(seed, client):
    req = urllib.request.Request(
        BASE + "/api/crawl",
        data=json.dumps({"seedUrl": seed}).encode(),
        headers={"Content-Type": "application/json", "X-Client-Id": client},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=90) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]


for seed in SEEDS:
    print("SEED:", seed)
    s, b = crawl(seed, "ext-test")
    if s == 200:
        print("  crawled", b["pagesCrawledThisRequest"], "/", b["maxPagesPerRequest"],
              "| visited", b["visitedCount"], "| queue", b["queueRemaining"],
              "| dups", b["duplicatesSkipped"])
        for p in b["crawledPages"][:3]:
            print("   -", (p["title"] or "")[:70], "| status", p["statusCode"],
                  "| links", len(p["articleLinksFound"]))
        print("   discovered sample:", b["discoveredUrls"][:3])
    else:
        print("  HTTP", s, b)
