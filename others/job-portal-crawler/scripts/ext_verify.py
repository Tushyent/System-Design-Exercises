import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080"


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
        return e.code, e.read().decode()[:200]


for seed in ["https://www.amazon.jobs/en/search?base_query=engineer",
             "https://careers.zohocorp.com/jobs/"]:
    print("SEED:", seed)
    s, b = crawl(seed, "ext-test")
    if s == 200:
        print("  crawled", b["pagesCrawledThisRequest"], "visited", b["visitedCount"],
              "queue", b["queueRemaining"], "dups", b["duplicatesSkipped"])
        for p in b["crawledPages"][:3]:
            print("   -", p["title"][:70], "| links:", len(p["jobLinksFound"]))
        print("   discovered sample:", b["discoveredUrls"][:3])
    else:
        print("  HTTP", s, b)
