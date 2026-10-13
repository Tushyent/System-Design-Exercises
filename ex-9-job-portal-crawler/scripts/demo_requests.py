"""Quick demo: normal requests, burst requests, and refill behaviour."""
import sys
import time
import urllib.request
import urllib.error
import json

BASE = "http://localhost:8080"
SEED = BASE + "/mock/jobs"


def crawl(client):
    req = urllib.request.Request(
        BASE + "/api/crawl",
        data=json.dumps({"seedUrl": SEED}).encode(),
        headers={"Content-Type": "application/json", "X-Client-Id": client},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            body = json.loads(r.read().decode())
            return r.status, body.get("pagesCrawledThisRequest"), body.get("queueRemaining")
    except urllib.error.HTTPError as e:
        return e.code, None, None


def reset_bucket(client):
    req = urllib.request.Request(BASE + "/api/bucket/" + client, method="DELETE")
    urllib.request.urlopen(req, timeout=10).read()


def main(mode="all"):
    client = "demo-client"
    if mode in ("all", "normal"):
        reset_bucket(client)
        print("== normal: 3 requests, 1s apart (expect 3x200) ==")
        for i in range(3):
            print(" ", crawl(client))
            time.sleep(1)
    if mode in ("all", "burst"):
        burst = "burst-client"
        reset_bucket(burst)
        print("== burst: 8 rapid requests (expect 5x200 then 429) ==")
        for i in range(8):
            print("  #%d" % (i + 1), crawl(burst))
        print("== wait 6s (refill 1/s) then retry (expect 200) ==")
        time.sleep(6)
        print("  retry", crawl(burst))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "all")
