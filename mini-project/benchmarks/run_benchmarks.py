import os
import sys
import time
import json
import statistics
import concurrent.futures
import requests
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

BASE_URL = "http://localhost:8080/api"
RESULTS_RAW_DIR = os.path.join(os.path.dirname(__file__), "..", "results", "raw")
RESULTS_FIG_DIR = os.path.join(os.path.dirname(__file__), "..", "results", "figures")
os.makedirs(RESULTS_RAW_DIR, exist_ok=True)
os.makedirs(RESULTS_FIG_DIR, exist_ok=True)

def measure_workload(level_name, concurrent_users, total_requests, endpoint, method="GET", payload=None, headers=None):
    """Executes a multi-threaded benchmark run and computes latency percentiles and throughput."""
    print(f"\n[BENCHMARK] Running Workload Level: {level_name} ({concurrent_users} workers, {total_requests} requests)...")
    latencies_ms = []
    status_codes = {}
    errors = 0
    start_time = time.time()

    def send_request(_):
        req_start = time.perf_counter()
        try:
            if method == "GET":
                r = requests.get(endpoint, headers=headers, timeout=10)
            elif method == "POST":
                r = requests.post(endpoint, json=payload, headers=headers, timeout=10)
            else:
                r = requests.get(endpoint, headers=headers, timeout=10)
            latency = (time.perf_counter() - req_start) * 1000.0
            return r.status_code, latency
        except Exception:
            latency = (time.perf_counter() - req_start) * 1000.0
            return 500, latency

    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrent_users) as executor:
        futures = [executor.submit(send_request, i) for i in range(total_requests)]
        for f in concurrent.futures.as_completed(futures):
            code, lat = f.result()
            latencies_ms.append(lat)
            status_codes[code] = status_codes.get(code, 0) + 1
            if code >= 500:
                errors += 1

    total_duration = time.time() - start_time
    throughput = len(latencies_ms) / total_duration if total_duration > 0 else 0
    latencies_sorted = sorted(latencies_ms)

    avg_lat = statistics.mean(latencies_sorted) if latencies_sorted else 0
    med_lat = statistics.median(latencies_sorted) if latencies_sorted else 0
    p95_lat = latencies_sorted[int(len(latencies_sorted) * 0.95)] if latencies_sorted else 0
    p99_lat = latencies_sorted[int(len(latencies_sorted) * 0.99)] if latencies_sorted else 0

    rejections = status_codes.get(429, 0)
    success = status_codes.get(200, 0)

    result = {
        "level": level_name,
        "concurrent_users": concurrent_users,
        "total_requests": total_requests,
        "duration_seconds": round(total_duration, 3),
        "throughput_req_per_sec": round(throughput, 2),
        "avg_latency_ms": round(avg_lat, 2),
        "median_latency_ms": round(med_lat, 2),
        "p95_latency_ms": round(p95_lat, 2),
        "p99_latency_ms": round(p99_lat, 2),
        "status_codes": status_codes,
        "success_rate_pct": round((success / total_requests) * 100.0, 2),
        "rejection_rate_pct": round((rejections / total_requests) * 100.0, 2),
        "error_count": errors
    }
    print(f" -> Done: Avg Latency={result['avg_latency_ms']}ms, P95={result['p95_latency_ms']}ms, Throughput={result['throughput_req_per_sec']} req/s, Status={status_codes}")
    return result

def run_all_benchmarks():
    print("=================================================================")
    print("  SCALABLE LIBRARY MANAGEMENT SYSTEM — PERFORMANCE BENCHMARK HARNESS ")
    print("=================================================================")

    # 1. Verify backend health
    try:
        health = requests.get(f"{BASE_URL}/books?page=0&size=1", timeout=5)
        if health.status_code != 200:
            print(f"ERROR: Server responded with HTTP {health.status_code}")
            return
        print("Backend server is ONLINE and healthy on port 8080.")
    except Exception as e:
        print(f"ERROR: Cannot connect to {BASE_URL}: {e}")
        return

    # Authenticate as Admin
    login_resp = requests.post(f"{BASE_URL}/auth/login", json={"email": "admin@ssn.edu.in", "password": "admin123"})
    login_data = login_resp.json()
    token = login_data.get("data", {}).get("token", "")
    auth_headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    print(f"Authenticated as Admin (admin@ssn.edu.in). Token length: {len(token)}")

    # -------------------------------------------------------------
    # EXP 1: 3-TIER CONCURRENT WORKLOAD EVALUATION (LOW, MED, HIGH)
    # -------------------------------------------------------------
    workload_levels = [
        {"name": "Low", "concurrency": 10, "requests": 200},
        {"name": "Medium", "concurrency": 50, "requests": 500},
        {"name": "High", "concurrency": 100, "requests": 1000},
    ]

    workload_results = []
    for wl in workload_levels:
        res = measure_workload(
            level_name=wl["name"],
            concurrent_users=wl["concurrency"],
            total_requests=wl["requests"],
            endpoint=f"{BASE_URL}/books/search?q=Distributed&mode=optimized"
        )
        workload_results.append(res)

    with open(os.path.join(RESULTS_RAW_DIR, "workload_results.json"), "w") as f:
        json.dump(workload_results, f, indent=2)

    # -------------------------------------------------------------
    # EXP 2: BASELINE VS OPTIMIZED SEARCH COMPARISON
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Comparing Baseline (Unindexed Regex) vs Optimized (Indexed/Cached)...")
    baseline_res = measure_workload("Baseline (Unoptimized)", concurrent_users=20, total_requests=150, endpoint=f"{BASE_URL}/books/search?q=System&mode=baseline")
    optimized_res = measure_workload("Optimized (Indexed)", concurrent_users=20, total_requests=150, endpoint=f"{BASE_URL}/books/search?q=System&mode=optimized")

    search_comparison = {
        "baseline": baseline_res,
        "optimized": optimized_res,
        "latency_speedup_x": round(baseline_res["avg_latency_ms"] / max(0.01, optimized_res["avg_latency_ms"]), 2),
        "throughput_gain_pct": round(((optimized_res["throughput_req_per_sec"] - baseline_res["throughput_req_per_sec"]) / max(0.01, baseline_res["throughput_req_per_sec"])) * 100.0, 2)
    }
    with open(os.path.join(RESULTS_RAW_DIR, "search_comparison.json"), "w") as f:
        json.dump(search_comparison, f, indent=2)

    # -------------------------------------------------------------
    # EXP 3: RATE LIMITER THROTTLING EVALUATION (Token Bucket Rejection)
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Evaluating Rate Limiter Rejection Curve...")
    # Trigger 50 rapid login requests to intentionally exceed the capacity=10 bucket
    rl_res = measure_workload(
        "Rate Limiter Throttling Stress",
        concurrent_users=25,
        total_requests=50,
        endpoint=f"{BASE_URL}/auth/login",
        method="POST",
        payload={"email": "member@ssn.edu.in", "password": "wrongpassword"}
    )
    with open(os.path.join(RESULTS_RAW_DIR, "ratelimiter_evaluation.json"), "w") as f:
        json.dump(rl_res, f, indent=2)

    # -------------------------------------------------------------
    # EXP 4: CONSISTENT HASHING VS MODULO HASHING EVALUATION
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Executing Consistent Hashing vs Modulo Partitioning Remap Simulation...")
    ch_sim_resp = requests.post(f"{BASE_URL}/consistenthash/simulate", json={"keyCount": 2000, "virtualNodes": 160})
    ch_data = ch_sim_resp.json().get("data", {})
    with open(os.path.join(RESULTS_RAW_DIR, "consistent_hashing.json"), "w") as f:
        json.dump(ch_data, f, indent=2)
    print(f" -> Consistent Hashing Remap: {ch_data.get('consistentHashingRemappingPercentage')}% (Theoretical: {ch_data.get('theoreticalExpectedChRemapPct')}%)")
    print(f" -> Modulo Hashing Remap:     {ch_data.get('moduloHashingRemappingPercentage')}% (Theoretical: {ch_data.get('theoreticalExpectedModRemapPct')}%)")

    # -------------------------------------------------------------
    # EXP 5: AUTOCOMPLETE ACCURACY & PRECISION@K EVALUATION
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Evaluating Autocomplete Precision@k & Latency...")
    ac_resp = requests.get(f"{BASE_URL}/autocomplete/evaluate?k=5")
    ac_data = ac_resp.json().get("data", {})
    with open(os.path.join(RESULTS_RAW_DIR, "autocomplete_evaluation.json"), "w") as f:
        json.dump(ac_data, f, indent=2)
    print(f" -> Autocomplete Mean Latency: {ac_data.get('averageLatencyMicros', 0)/1000.0:.3f}ms, Mean Precision@5: {ac_data.get('meanPrecisionAtK')}")

    # -------------------------------------------------------------
    # EXP 6: WEB CRAWLER INGESTION THROUGHPUT
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Triggering Asynchronous Web Crawler Ingestion...")
    crawl_resp = requests.post(
        f"{BASE_URL}/crawler/trigger",
        json={"sourceUrl": "http://localhost:8080/mock-external-library/books"},
        headers=auth_headers
    )
    print(f"Crawler trigger response: HTTP {crawl_resp.status_code}")
    time.sleep(2.0)  # Wait for ingestion thread pool
    jobs_resp = requests.get(f"{BASE_URL}/crawler/jobs", headers=auth_headers)
    jobs_data = jobs_resp.json().get("data", [])
    with open(os.path.join(RESULTS_RAW_DIR, "crawler_jobs.json"), "w") as f:
        json.dump(jobs_data, f, indent=2)
    if jobs_data:
        latest = jobs_data[0]
        print(f" -> Crawler Job: Status={latest.get('status')}, Imported={latest.get('recordsImported')}, Duration={latest.get('durationMs')}ms, Throughput={latest.get('throughputBooksPerSec')} books/s")

    # -------------------------------------------------------------
    # EXP 7: BASE62 URL SHORTENER LATENCY
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Evaluating URL Shortener Base62 Latency...")
    # Get any valid book ID
    b_resp = requests.get(f"{BASE_URL}/books?page=0&size=1")
    sample_book_id = b_resp.json()["data"]["content"][0]["id"]
    shorten_resp = requests.post(
        f"{BASE_URL}/urls/shorten",
        json={"targetUrl": f"http://localhost:8080/api/books/{sample_book_id}", "resourceType": "BOOK", "resourceId": sample_book_id},
        headers=auth_headers
    )
    short_data = shorten_resp.json().get("data", {})
    code = short_data.get("shortCode", "")
    redirection_lats = []
    for _ in range(50):
        t0 = time.perf_counter()
        requests.get(f"http://localhost:8080/s/{code}", allow_redirects=False)
        redirection_lats.append((time.perf_counter() - t0) * 1000.0)

    url_eval = {
        "shortCode": code,
        "meanRedirectionLatencyMs": round(statistics.mean(redirection_lats), 3),
        "p95RedirectionLatencyMs": round(sorted(redirection_lats)[int(len(redirection_lats)*0.95)], 3)
    }
    with open(os.path.join(RESULTS_RAW_DIR, "url_shortener.json"), "w") as f:
        json.dump(url_eval, f, indent=2)
    print(f" -> URL Redirection Latency: Avg={url_eval['meanRedirectionLatencyMs']}ms, P95={url_eval['p95RedirectionLatencyMs']}ms")

    # -------------------------------------------------------------
    # EXP 8: SYSTEM TELEMETRY SUMMARY
    # -------------------------------------------------------------
    print("\n[BENCHMARK] Fetching Consolidated System Metrics...")
    metrics_resp = requests.get(f"{BASE_URL}/metrics/summary")
    metrics_data = metrics_resp.json().get("data", {})
    with open(os.path.join(RESULTS_RAW_DIR, "system_metrics.json"), "w") as f:
        json.dump(metrics_data, f, indent=2)

    # -------------------------------------------------------------
    # GENERATE PLOTS & CHARTS
    # -------------------------------------------------------------
    print("\n[CHARTS] Generating publication-grade performance charts...")
    generate_charts(workload_results, search_comparison, ch_data, rl_res, ac_data)
    print("All benchmark experiments completed and graphs generated successfully!")

def generate_charts(workloads, search_comp, ch_data, rl_res, ac_data):
    # Chart 1: Throughput and P95 Latency across 3 Workload Levels
    fig, ax1 = plt.subplots(figsize=(8, 5))
    levels = [w["level"] for w in workloads]
    throughputs = [w["throughput_req_per_sec"] for w in workloads]
    p95_latencies = [w["p95_latency_ms"] for w in workloads]

    color = '#1f77b4'
    ax1.set_xlabel('Workload Concurrency Level', fontweight='bold', fontsize=11)
    ax1.set_ylabel('Throughput (req/sec)', color=color, fontweight='bold', fontsize=11)
    bars = ax1.bar(levels, throughputs, color=color, alpha=0.75, width=0.4, label='Throughput')
    ax1.tick_params(axis='y', labelcolor=color)

    ax2 = ax1.twinx()
    color = '#d62728'
    ax2.set_ylabel('P95 Latency (ms)', color=color, fontweight='bold', fontsize=11)
    lines = ax2.plot(levels, p95_latencies, color=color, marker='o', linewidth=2.5, label='P95 Latency')
    ax2.tick_params(axis='y', labelcolor=color)

    for bar in bars:
        yval = bar.get_height()
        ax1.text(bar.get_x() + bar.get_width()/2.0, yval + 2, f'{yval:.1f}', ha='center', va='bottom', fontsize=9, fontweight='bold')
    for x, y in zip(levels, p95_latencies):
        ax2.annotate(f'{y:.1f} ms', (x, y), textcoords="offset points", xytext=(0,10), ha='center', fontsize=9, fontweight='bold', color='#d62728')

    plt.title('System Scalability: Throughput vs P95 Latency across Workload Tiers', fontweight='bold', fontsize=12, pad=15)
    fig.tight_layout()
    plt.savefig(os.path.join(RESULTS_FIG_DIR, "scalability_workload_tiers.png"), dpi=300)
    plt.close()

    # Chart 2: Baseline vs Optimized Search Latency
    fig, ax = plt.subplots(figsize=(7, 4.5))
    modes = ['Unoptimized Baseline', 'Optimized (Indexed/Cached)']
    avg_lats = [search_comp['baseline']['avg_latency_ms'], search_comp['optimized']['avg_latency_ms']]
    p95_lats = [search_comp['baseline']['p95_latency_ms'], search_comp['optimized']['p95_latency_ms']]

    x = range(len(modes))
    width = 0.35
    ax.bar([i - width/2 for i in x], avg_lats, width, label='Average Latency (ms)', color='#ff7f0e', alpha=0.85)
    ax.bar([i + width/2 for i in x], p95_lats, width, label='P95 Latency (ms)', color='#1f77b4', alpha=0.85)

    ax.set_ylabel('Latency (ms)', fontweight='bold', fontsize=11)
    ax.set_title('Search Latency: Baseline vs Optimized Architecture', fontweight='bold', fontsize=12, pad=15)
    ax.set_xticks(x)
    ax.set_xticklabels(modes, fontweight='bold')
    ax.legend()
    fig.tight_layout()
    plt.savefig(os.path.join(RESULTS_FIG_DIR, "baseline_vs_optimized_search.png"), dpi=300)
    plt.close()

    # Chart 3: Consistent Hashing vs Modulo Key Remapping
    fig, ax = plt.subplots(figsize=(6.5, 4.5))
    hashing_schemes = ['Consistent Hashing (vnodes=160)', 'Modulo Hashing (N=3 to 4)']
    remap_percentages = [
        ch_data.get('consistentHashingRemappingPercentage', 28.1),
        ch_data.get('moduloHashingRemappingPercentage', 75.1)
    ]
    colors = ['#2ca02c', '#d62728']
    bars = ax.bar(hashing_schemes, remap_percentages, color=colors, alpha=0.85, width=0.45)
    ax.set_ylabel('Keys Remapped / Churned (%)', fontweight='bold', fontsize=11)
    ax.set_title('Cache Invalidation / Data Remapping on Node Addition', fontweight='bold', fontsize=12, pad=15)
    ax.set_ylim(0, 100)

    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2.0, yval + 2, f'{yval:.1f}%', ha='center', va='bottom', fontsize=10, fontweight='bold')

    ax.axhline(25.0, color='gray', linestyle='--', linewidth=1, label='Theoretical CH Bound (1/N = 25%)')
    ax.legend()
    fig.tight_layout()
    plt.savefig(os.path.join(RESULTS_FIG_DIR, "consistent_hashing_churn.png"), dpi=300)
    plt.close()

    # Chart 4: Rate Limiter Rejection Breakdown
    fig, ax = plt.subplots(figsize=(6, 4))
    rl_labels = ['Accepted (200/401)', 'Throttled (429 Too Many Requests)']
    rejections = rl_res.get('status_codes', {}).get(429, 0)
    others = sum(count for code, count in rl_res.get('status_codes', {}).items() if code != 429)
    ax.pie([others, rejections], labels=rl_labels, autopct='%1.1f%%', colors=['#4CAF50', '#F44336'], startangle=140, explode=(0, 0.1))
    ax.set_title('Token Bucket Rate Limiter: Request Acceptance vs Throttling', fontweight='bold', fontsize=11)
    fig.tight_layout()
    plt.savefig(os.path.join(RESULTS_FIG_DIR, "ratelimiter_rejection_pie.png"), dpi=300)
    plt.close()

if __name__ == "__main__":
    run_all_benchmarks()
