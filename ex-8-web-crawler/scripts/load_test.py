#!/usr/bin/env python3
"""Automated demonstration, benchmark, and concurrent stress testing
for Lab Exercise 8: Web Crawler for Automated Web Content Discovery.

Usage:
    python scripts/load_test.py demo         # Step-by-step verification demo
    python scripts/load_test.py benchmark    # Cache hit vs miss latency benchmark
    python scripts/load_test.py concurrent   # Multi-threaded stress load test
    python scripts/load_test.py all          # Run all suites sequentially
"""

import sys
import time
import json
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://localhost:8080"
API = f"{BASE_URL}/api/crawler"

def check_server():
    try:
        r = requests.get(f"{API}/status", timeout=3)
        return r.status_code == 200
    except Exception:
        return False

def run_demo():
    print("\n" + "=" * 70)
    print("DEMO: Automated Web Content Discovery, Deduplication & Search")
    print("=" * 70)

    if not check_server():
        print(f"Error: Spring Boot application is not running at {BASE_URL}.")
        print("Please start the application first.")
        sys.exit(1)

    print("\n[Step 1] Resetting crawler state, search index, and clearing Redis storage...")
    r = requests.post(f"{API}/reset")
    print(f"  Response ({r.status_code}): {r.json().get('message')}")

    print("\n[Step 2] Initiating asynchronous crawl session from Root Seed...")
    seed_url = f"{BASE_URL}/mock-web/index.html"
    payload = {
        "seedUrls": [seed_url],
        "maxDepth": 3,
        "maxPages": 50,
        "workers": 4,
        "politenessDelayMs": 30,
        "respectRobotsTxt": True,
        "discoverSitemaps": True,
        "detectDuplicateContent": True
    }
    r = requests.post(f"{API}/start", json=payload)
    data = r.json()
    print(f"  Job Started: {data.get('jobId')} | Status: {data.get('status')} | Mode: {data.get('storageMode')}")

    print("\n[Step 3] Monitoring crawl progress in real-time...")
    for _ in range(30):
        time.sleep(0.5)
        st = requests.get(f"{API}/status").json()
        print(f"  Crawled: {st['totalCrawled']:2d} | Queue: {st['queueSize']:2d} | Visited: {st['visitedCount']:2d} | "
              f"URL Dups: {st['duplicateUrlsAvoided']:2d} | Content Dups: {st.get('duplicateContentAvoided', 0):2d} | "
              f"Robots Blocked: {st.get('robotsDisallowedCount', 0):2d} | Status: {st['status']}")
        if st["status"] in ("COMPLETED", "STOPPED"):
            break

    print("\n[Step 4] Final Crawl Summary & Architecture Verification:")
    final_st = requests.get(f"{API}/status").json()
    print(f"  • Total Pages Discovered      : {final_st['totalDiscovered']}")
    print(f"  • Total Pages Crawled         : {final_st['totalCrawled']}")
    print(f"  • Visited Set Size (Redis)    : {final_st['visitedCount']}")
    print(f"  • URL Loops Prevented         : {final_st['duplicateUrlsAvoided']}")
    print(f"  • Content Checksum Dups Found : {final_st.get('duplicateContentAvoided', 0)}")
    print(f"  • Robots.txt Disallows Obeyed : {final_st.get('robotsDisallowedCount', 0)}")
    print(f"  • Sitemaps Discovered & Read  : {final_st.get('sitemapsDiscovered', 0)}")
    print(f"  • Execution Time              : {final_st['elapsedTimeMs']} ms")
    print(f"  • Throughput                  : {final_st['throughputPagesPerSec']} pages/sec")

    print("\n[Step 5] Automated Web Content Discovery: Full-Text Search & Multi-Strategy Ranking")
    search_terms = ["quantum computing", "artificial intelligence", "football"]
    for q in search_terms:
        s_res = requests.get(f"{API}/search?q={requests.utils.quote(q)}&sortBy=COMPOSITE").json()
        print(f"  Search '{q}' (COMPOSITE): {s_res['totalMatches']} matches | Corpus Mentions: {s_res.get('totalOccurrencesInCorpus', 0)} | Execution: {s_res['searchTimeMs']} ms")
        if s_res.get('categoryDistribution'):
            print(f"    ↳ Categories: {s_res['categoryDistribution']}")
        if s_res.get('relatedKeywords'):
            print(f"    ↳ Related Entities: {s_res['relatedKeywords'][:5]}")
        for match in s_res["results"][:2]:
            print(f"    ↳ Title : {match['title']} [{match.get('category', 'General')}]")
            print(f"      URL   : {match['url']}")
            print(f"      Score : {match['relevanceScore']:.2f} (BM25: {match.get('bm25Score', 0):.2f}, PageRank: {match.get('pageRankScore', 0):.1f})")
            print(f"      Snippet: {match['snippet']}")

    print("\n[Step 5b] Ranking Strategy Comparison (COMPOSITE vs PAGERANK vs BM25)")
    test_kw = "quantum"
    for strategy in ["COMPOSITE", "PAGERANK", "BM25"]:
        res_strat = requests.get(f"{API}/search?q={test_kw}&sortBy={strategy}").json()
        top_hit = res_strat["results"][0] if res_strat["results"] else {}
        print(f"  Strategy {strategy:9s} -> Top: '{top_hit.get('title', 'N/A')}' (Score: {top_hit.get('relevanceScore', 0):.2f}, PR: {top_hit.get('pageRankScore', 0):.1f})")

    print("\n[Step 5c] Vocabulary Autocomplete Queries")
    for pfx in ["qua", "algo", "arti", "crick"]:
        ac_res = requests.get(f"{API}/autocomplete?prefix={pfx}&limit=5").json()
        terms = [f"{item['term']} ({item['frequency']})" for item in ac_res]
        print(f"  Prefix '{pfx}*' -> {terms}")

    print("\n[Step 5d] Google PageRank Link Authority Leaderboard")
    pr_res = requests.get(f"{API}/pagerank").json()
    print(f"  • Total Graph Nodes Scored : {pr_res.get('totalPages', 0)} (Damping Factor d={pr_res.get('dampingFactor', 0.85)})")
    for rank_idx, item in enumerate(pr_res.get("leaderboard", [])[:5], start=1):
        print(f"    #{rank_idx} [PR: {item['pageRankScore']:.2f}] {item['title']} -> {item['url']}")

    print("\n[Step 5e] Discovered Topics & Taxonomy Extraction")
    topic_res = requests.get(f"{API}/topics").json()
    print(f"  • Category Distribution: {topic_res.get('categories')}")
    top_ents = [f"{e['keyword']} ({e['documentCount']} docs)" for e in topic_res.get('topEntities', [])[:6]]
    print(f"  • Top Discovered Entities: {top_ents}")

    print("\n[Step 6] Discovered Content Export Verification")
    r_json = requests.get(f"{API}/export?format=json")
    print(f"  • Export JSON: HTTP {r_json.status_code} | Pages: {len(r_json.json())}")
    r_csv = requests.get(f"{API}/export?format=csv")
    csv_lines = r_csv.text.strip().split("\n")
    print(f"  • Export CSV : HTTP {r_csv.status_code} | Total Lines: {len(csv_lines)} (Header + {len(csv_lines)-1} rows)")

    print("\n[Step 7] Single-URL Synchronous Fetch: Cache Miss vs Cache Hit")
    test_url = f"{BASE_URL}/mock-web/tech.html"
    
    # Query cached page
    r_cache = requests.post(f"{API}/crawl-single?url={test_url}").json()
    print(f"  1. Cache Query: Source={r_cache['source']} | Title='{r_cache['title']}' | Latency={r_cache['crawlTimeMs']} ms | Links={r_cache['linksCount']}")

    # Clear and fresh fetch
    requests.post(f"{API}/reset")
    r_fresh = requests.post(f"{API}/crawl-single?url={test_url}").json()
    print(f"  2. Fresh Fetch: Source={r_fresh['source']} | Title='{r_fresh['title']}' | Latency={r_fresh['crawlTimeMs']} ms | Links={r_fresh['linksCount']}")

    # Re-query
    r_cached2 = requests.post(f"{API}/crawl-single?url={test_url}").json()
    print(f"  3. Re-query   : Source={r_cached2['source']} | Title='{r_cached2['title']}' | Latency={r_cached2['crawlTimeMs']} ms | Links={r_cached2['linksCount']}")

    print("\nDemo completed successfully!")

def run_benchmark():
    print("\n" + "=" * 70)
    print("BENCHMARK: Latency Comparison (Fresh Network Fetch vs Redis Cache)")
    print("=" * 70)

    if not check_server():
        print(f"Error: Spring Boot application is not running at {BASE_URL}.")
        sys.exit(1)

    urls = [
        f"{BASE_URL}/mock-web/index.html",
        f"{BASE_URL}/mock-web/tech.html",
        f"{BASE_URL}/mock-web/ai.html",
        f"{BASE_URL}/mock-web/cloud.html",
        f"{BASE_URL}/mock-web/sports.html"
    ]

    print("\n1. Measuring Fresh Live Crawl Latencies (Cache Bypassed)...")
    fresh_times = []
    for u in urls:
        requests.post(f"{API}/reset")
        t0 = time.perf_counter()
        r = requests.post(f"{API}/crawl-single?url={u}")
        rtt = (time.perf_counter() - t0) * 1000.0
        fresh_times.append(rtt)
        d = r.json()
        print(f"  Fresh Fetch [{d['source']}]: {u} -> {rtt:.2f} ms")

    # Prime cache for all benchmark URLs
    for u in urls:
        requests.post(f"{API}/crawl-single?url={u}")

    print("\n2. Measuring Redis Cached Retrieval Latencies...")
    cached_times = []
    for u in urls:
        t0 = time.perf_counter()
        r = requests.post(f"{API}/crawl-single?url={u}")
        rtt = (time.perf_counter() - t0) * 1000.0
        cached_times.append(rtt)
        d = r.json()
        print(f"  Cache Hit   [{d['source']}]: {u} -> {rtt:.2f} ms")

    avg_fresh = sum(fresh_times) / len(fresh_times)
    avg_cached = sum(cached_times) / len(cached_times)
    speedup = avg_fresh / avg_cached if avg_cached > 0 else 0

    print("\n" + "-" * 50)
    print("BENCHMARK RESULTS:")
    print(f"  • Average Fresh Live Fetch : {avg_fresh:.2f} ms")
    print(f"  • Average Redis Cache Hit  : {avg_cached:.2f} ms")
    print(f"  • Speedup Factor           : {speedup:.1f}x Faster")
    print("-" * 50)

def run_concurrent(concurrency=50, total_requests=200):
    print("\n" + "=" * 70)
    print(f"CONCURRENCY STRESS TEST: {concurrency} Workers | {total_requests} Requests")
    print("=" * 70)

    if not check_server():
        print(f"Error: Spring Boot application is not running at {BASE_URL}.")
        sys.exit(1)

    url = f"{BASE_URL}/mock-web/index.html"
    # Prime cache
    requests.post(f"{API}/crawl-single?url={url}")

    latencies = []
    success_count = 0

    def make_request():
        t0 = time.perf_counter()
        try:
            r = requests.post(f"{API}/crawl-single?url={url}", timeout=5)
            rtt = (time.perf_counter() - t0) * 1000.0
            return (r.status_code == 200, rtt)
        except Exception:
            return (False, 0)

    t_start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(make_request) for _ in range(total_requests)]
        for f in as_completed(futures):
            ok, rtt = f.result()
            if ok:
                success_count += 1
                latencies.append(rtt)

    total_time = time.perf_counter() - t_start
    throughput = total_requests / total_time if total_time > 0 else 0

    latencies.sort()
    p50 = latencies[int(len(latencies) * 0.50)] if latencies else 0
    p95 = latencies[int(len(latencies) * 0.95)] if latencies else 0
    p99 = latencies[int(len(latencies) * 0.99)] if latencies else 0

    print("\nCONCURRENT TEST RESULTS:")
    print(f"  • Successful Requests : {success_count}/{total_requests} ({success_count/total_requests*100:.1f}%)")
    print(f"  • Total Duration     : {total_time:.2f} s")
    print(f"  • Throughput         : {throughput:.1f} req/s")
    print(f"  • Median (p50) RTT   : {p50:.2f} ms")
    print(f"  • 95th Percentile RTT: {p95:.2f} ms")
    print(f"  • 99th Percentile RTT: {p99:.2f} ms")

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    if mode == "demo":
        run_demo()
    elif mode == "benchmark":
        run_benchmark()
    elif mode == "concurrent":
        run_concurrent()
    elif mode == "all":
        run_demo()
        run_benchmark()
        run_concurrent()
    else:
        print(f"Unknown mode: {mode}. Use demo | benchmark | concurrent | all")
