#!/usr/bin/env python3
"""Load/demo driver for the token-bucket rate limiter lab (Ex 6).

Generates NORMAL, BURST and CONCURRENT traffic against a running Spring Boot app
and records the observed status code + remaining tokens for every request so the
rate-limiting behaviour can be charted and analysed.

Usage:
    python scripts/load_test.py reset  --client demo
    python scripts/load_test.py normal --client demo --requests 10 --interval 0.25
    python scripts/load_test.py burst  --client demo --requests 50
    python scripts/load_test.py drain  --client demo
    python scripts/load_test.py concurrent --client demo --threads 20 --per-thread 20 \
        --capacity 10 --rate 5
    python scripts/load_test.py status --client demo

Stdlib only (urllib + threading), so it runs anywhere.
"""

import argparse
import csv
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = os.environ.get("RATE_LIMITER_BASE", "http://localhost:8080")
REPORTS = os.path.join(os.path.dirname(__file__), "..", "reports")


def request(method, path, payload=None, client=None):
    url = BASE + path
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = json.loads(resp.read().decode() or "{}")
            return resp.status, body, (time.time() - t0) * 1000.0
    except urllib.error.HTTPError as err:
        body = json.loads(err.read().decode() or "{}")
        return err.code, body, (time.time() - t0) * 1000.0


def get(client, path="/api/hello"):
    return request("GET", f"{path}?clientId={client}", client=client)


def reset(client):
    status, _, _ = request("DELETE", f"/api/bucket/{client}")
    return status


def write_rows(mode, rows):
    os.makedirs(REPORTS, exist_ok=True)
    path = os.path.join(REPORTS, f"{mode}.csv")
    with open(path, "w", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["seq", "status", "remaining", "elapsed_ms"])
        writer.writeheader()
        writer.writerows(rows)
    return path


def cmd_normal(args):
    reset(args.client)
    rows, allowed, rejected = [], 0, 0
    print(f"--- NORMAL traffic: {args.requests} requests @ {args.interval}s interval (under the {args.rate}/s refill) ---")
    for i in range(1, args.requests + 1):
        status, body, ms = get(args.client)
        remaining = body.get("remaining", 0) if status == 200 else 0
        rows.append({"seq": i, "status": status, "remaining": remaining, "elapsed_ms": round(ms, 1)})
        if status == 200:
            allowed += 1
        else:
            rejected += 1
        print(f"  req #{i:3d}  -> HTTP {status} {'ALLOWED' if status == 200 else 'REJECTED':8s} remaining={remaining:5.1f} ({ms:6.1f} ms)")
        if i < args.requests:
            time.sleep(args.interval)
    write_rows("normal", rows)
    print(f"  NORMAL summary: allowed={allowed} rejected={rejected} (capacity {args.capacity}, refill {args.rate}/s)")


def cmd_burst(args):
    reset(args.client)
    rows = []
    lock = threading.Lock()
    allowed, rejected = 0, 0
    print(f"--- BURST traffic: {args.requests} simultaneous requests (capacity {args.capacity}) ---")

    def one(seq):
        nonlocal allowed, rejected
        status, body, ms = get(args.client)
        remaining = body.get("remaining", 0) if status == 200 else 0
        with lock:
            rows.append({"seq": seq, "status": status, "remaining": remaining, "elapsed_ms": round(ms, 1)})
            if status == 200:
                allowed += 1
            else:
                rejected += 1
            print(f"  burst #{seq:3d} -> HTTP {status} {'ALLOWED' if status == 200 else 'REJECTED':8s} remaining={remaining:5.1f} ({ms:6.1f} ms)")

    with ThreadPoolExecutor(max_workers=args.requests) as pool:
        list(pool.map(one, range(1, args.requests + 1)))

    rows.sort(key=lambda r: r["seq"])
    write_rows("burst", rows)
    print(f"  BURST summary: allowed={allowed} rejected={rejected} -> burst capacity held at ~{args.capacity}")


def cmd_drain(args):
    reset(args.client)
    print(f"--- DRAIN: request one-by-one until HTTP 429 (capacity {args.capacity}) ---")
    rows = []
    for i in range(1, args.capacity + 8):
        status, body, ms = get(args.client)
        remaining = body.get("remaining", 0) if status == 200 else 0
        rows.append({"seq": i, "status": status, "remaining": remaining, "elapsed_ms": round(ms, 1)})
        print(f"  req #{i:3d}  -> HTTP {status} remaining={remaining:5.1f}")
        if status == 429:
            print(f"  DRAIN: burst capacity observed = {i - 1} requests before HTTP 429")
            break
    write_rows("drain", rows)


def cmd_concurrent(args):
    payload = {
        "clientId": args.client,
        "threads": args.threads,
        "requestsPerThread": args.per_thread,
        "capacity": args.capacity,
        "refillPerSecond": args.rate,
    }
    print(f"--- CONCURRENT test: {args.threads} threads x {args.per_thread} req (capacity {args.capacity}, refill {args.rate}/s) ---")
    status, body, ms = request("POST", "/api/test/concurrent", payload=payload)
    print(json.dumps(body, indent=2))
    with open(os.path.join(REPORTS, "concurrent.json"), "w") as fh:
        json.dump(body, fh, indent=2)
    return body


def cmd_status(args):
    status, body, ms = request("GET", f"/api/bucket/{args.client}")
    print(json.dumps(body, indent=2))


def cmd_reset(args):
    print(f"reset {args.client} -> HTTP {reset(args.client)}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="mode", required=True)

    p = sub.add_parser("reset"); p.add_argument("--client", default="demo-user"); p.set_defaults(fn=cmd_reset)
    p = sub.add_parser("status"); p.add_argument("--client", default="demo-user"); p.set_defaults(fn=cmd_status)
    p = sub.add_parser("normal")
    p.add_argument("--client", default="demo-user"); p.add_argument("--requests", type=int, default=10)
    p.add_argument("--interval", type=float, default=0.25)
    p.add_argument("--capacity", type=int, default=10); p.add_argument("--rate", type=float, default=5)
    p.set_defaults(fn=cmd_normal)
    p = sub.add_parser("burst")
    p.add_argument("--client", default="demo-user"); p.add_argument("--requests", type=int, default=50)
    p.add_argument("--capacity", type=int, default=10); p.add_argument("--rate", type=float, default=5)
    p.set_defaults(fn=cmd_burst)
    p = sub.add_parser("drain")
    p.add_argument("--client", default="demo-user"); p.add_argument("--capacity", type=int, default=10)
    p.set_defaults(fn=cmd_drain)
    p = sub.add_parser("concurrent")
    p.add_argument("--client", default="demo-user"); p.add_argument("--threads", type=int, default=20)
    p.add_argument("--per-thread", type=int, default=20); p.add_argument("--capacity", type=int, default=10)
    p.add_argument("--rate", type=float, default=5)
    p.set_defaults(fn=cmd_concurrent)

    args = ap.parse_args()
    os.makedirs(REPORTS, exist_ok=True)
    args.fn(args)


if __name__ == "__main__":
    main()
