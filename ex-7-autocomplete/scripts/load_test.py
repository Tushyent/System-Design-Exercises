#!/usr/bin/env python3
"""Load/demo driver for the Autocomplete Search System (Ex 7).

Performs cache hit vs cache miss latency comparisons, concurrent search simulations,
and search frequency updates with automatic cache eviction.

Usage:
    python scripts/load_test.py demo
    python scripts/load_test.py benchmark
    python scripts/load_test.py concurrent --threads 10 --requests 100
"""

import argparse
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("AUTOCOMPLETE_BASE", "http://localhost:8080")

def api_request(method, path, payload=None):
    url = BASE + path
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            content = resp.read().decode()
            try:
                body = json.loads(content or "{}")
            except json.JSONDecodeError:
                body = {"message": content}
            return resp.status, body, (time.time() - t0) * 1000.0
    except urllib.error.HTTPError as err:
        content = err.read().decode()
        try:
            body = json.loads(content or "{}")
        except json.JSONDecodeError:
            body = {"message": content}
        return err.code, body, (time.time() - t0) * 1000.0
    except Exception as e:
        print(f"Error connecting to server: {e}")
        return 0, {}, 0.0

def get_suggestions(prefix, limit=5):
    path = f"/api/autocomplete?prefix={urllib.parse.quote(prefix)}&limit={limit}"
    return api_request("GET", path)

def submit_search(word):
    payload = {"word": word}
    return api_request("POST", "/api/autocomplete/search", payload)

def run_demo():
    print("==================================================")
    print("      Autocomplete Search Cache & Trie Demo      ")
    print("==================================================")
    
    # 1. First search (Trie lookup / Cache Miss)
    print("\n[Step 1] Querying prefix 'app' (Expect TRIE lookup - Cache Miss)")
    status, res, rtt = get_suggestions("app")
    if status == 200:
        print(f"Status: {status} | Source: {res.get('source')} | Execution Time: {res.get('executionTimeMs')}ms | RTT: {rtt:.2f}ms")
        print("Suggestions returned:")
        for idx, sug in enumerate(res.get("suggestions", [])):
            print(f"  {idx+1}. {sug.get('word')} (freq: {sug.get('frequency')})")
    else:
        print(f"Failed: Status {status}")
        return

    # 2. Second search (Cache Hit)
    print("\n[Step 2] Querying prefix 'app' again (Expect REDIS lookup - Cache Hit)")
    status, res, rtt = get_suggestions("app")
    if status == 200:
        print(f"Status: {status} | Source: {res.get('source')} | Execution Time: {res.get('executionTimeMs')}ms | RTT: {rtt:.2f}ms")
        for idx, sug in enumerate(res.get("suggestions", [])):
            print(f"  {idx+1}. {sug.get('word')} (freq: {sug.get('frequency')})")

    # 3. Submit a new search to update frequency
    print("\n[Step 3] Submitting new search for 'apple' (Increments frequency + Evicts cache)")
    status, res, rtt = submit_search("apple")
    print(f"Status: {status} | Response: {res} | RTT: {rtt:.2f}ms")

    # 4. Third search (Cache Invalidation verification - Cache Miss)
    print("\n[Step 4] Querying prefix 'app' again (Expect TRIE lookup due to cache eviction)")
    status, res, rtt = get_suggestions("app")
    if status == 200:
        print(f"Status: {status} | Source: {res.get('source')} | Execution Time: {res.get('executionTimeMs')}ms | RTT: {rtt:.2f}ms")
        print("Suggestions returned (Note increased frequency for apple):")
        for idx, sug in enumerate(res.get("suggestions", [])):
            print(f"  {idx+1}. {sug.get('word')} (freq: {sug.get('frequency')})")

def run_benchmark():
    print("==================================================")
    print("      Autocomplete Performance Benchmark         ")
    print("==================================================")
    
    prefixes = ["tech", "soft", "micro", "web", "sys", "data", "cloud", "pro", "dev", "app"]
    
    print("\n--- Phase 1: Warmup & Cache Misses (Trie Queries) ---")
    miss_times = []
    for p in prefixes:
        status, res, rtt = get_suggestions(p)
        if status == 200:
            exec_time = res.get('executionTimeMs')
            print(f"Prefix: '{p:<6}' | Source: {res.get('source'):<5} | Server Time: {exec_time:>2}ms | RTT: {rtt:>5.2f}ms")
            miss_times.append(rtt)
            
    print("\n--- Phase 2: Cache Hits (Redis Queries) ---")
    hit_times = []
    for p in prefixes:
        status, res, rtt = get_suggestions(p)
        if status == 200:
            exec_time = res.get('executionTimeMs')
            print(f"Prefix: '{p:<6}' | Source: {res.get('source'):<5} | Server Time: {exec_time:>2}ms | RTT: {rtt:>5.2f}ms")
            hit_times.append(rtt)
            
    avg_miss = sum(miss_times) / len(miss_times) if miss_times else 0
    avg_hit = sum(hit_times) / len(hit_times) if hit_times else 0
    
    print("\n================ Benchmark Summary ================")
    print(f"Average Cache Miss (Trie) RTT: {avg_miss:.2f} ms")
    print(f"Average Cache Hit (Redis) RTT: {avg_hit:.2f} ms")
    if avg_hit > 0:
        speedup = avg_miss / avg_hit
        print(f"Performance Improvement: {speedup:.2f}x speedup")
    print("==================================================")

def run_concurrent(threads_count, requests_count):
    print("==================================================")
    print(f"      Concurrent Autocomplete Load Test          ")
    print(f"      Threads: {threads_count} | Total Queries: {requests_count}      ")
    print("==================================================")
    
    prefixes = ["app", "tech", "soft", "micro", "web", "sys", "data", "cloud", "pro", "dev"]
    results = []
    errors = 0
    lock = threading.Lock()
    
    def worker(num_queries):
        nonlocal errors
        import random
        for _ in range(num_queries):
            prefix = random.choice(prefixes)
            status, res, rtt = get_suggestions(prefix)
            if status == 200:
                with lock:
                    results.append((res.get("source"), rtt))
            else:
                with lock:
                    errors += 1

    queries_per_thread = requests_count // threads_count
    threads = []
    
    t0 = time.time()
    for _ in range(threads_count):
        t = threading.Thread(target=worker, args=(queries_per_thread,))
        threads.append(t)
        t.start()
        
    for t in threads:
        t.join()
    duration = (time.time() - t0) * 1000.0
    
    cache_hits = [r[1] for r in results if r[0] == "CACHE"]
    trie_queries = [r[1] for r in results if r[0] == "TRIE"]
    
    print(f"\nCompleted {len(results)} successful requests in {duration:.2f} ms")
    print(f"Errors: {errors}")
    print(f"Throughput: {len(results)/(duration/1000.0):.2f} requests/sec")
    print(f"Cache Hits: {len(cache_hits)} ({len(cache_hits)/max(1, len(results))*100:.1f}%)")
    print(f"Trie Queries (Misses): {len(trie_queries)} ({len(trie_queries)/max(1, len(results))*100:.1f}%)")
    if cache_hits:
        print(f"Average Cache Hit RTT: {sum(cache_hits)/len(cache_hits):.2f} ms")
    if trie_queries:
        print(f"Average Trie Query RTT: {sum(trie_queries)/len(trie_queries):.2f} ms")

def main():
    parser = argparse.ArgumentParser(description="Demo/Load test driver for Autocomplete System.")
    parser.add_argument("mode", choices=["demo", "benchmark", "concurrent"], help="Orchestration mode")
    parser.add_argument("--threads", type=int, default=10, help="Number of threads (concurrent mode only)")
    parser.add_argument("--requests", type=int, default=100, help="Total requests (concurrent mode only)")
    
    args = parser.parse_args()
    
    if args.mode == "demo":
        run_demo()
    elif args.mode == "benchmark":
        run_benchmark()
    elif args.mode == "concurrent":
        run_concurrent(args.threads, args.requests)

if __name__ == "__main__":
    main()
