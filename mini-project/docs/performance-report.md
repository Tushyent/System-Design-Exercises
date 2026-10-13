# Quantitative Performance Evaluation Report
## Scalable Library Management System (Problem Statement 3)
**Course**: UCS3513 — System Design Laboratory, SSN College of Engineering  
**Harness**: `benchmarks/run_benchmarks.py`  
**Raw Data Directory**: `results/raw/`  
**Figures Directory**: `results/figures/`  

---

## 1. Multi-Tier Workload Evaluation

The system was stress-tested across three concurrent workload levels on the search and availability endpoints using a multi-threaded benchmark harness (`ThreadPoolExecutor`):

| Metric | Workload Tier 1 (Low) | Workload Tier 2 (Medium) | Workload Tier 3 (High) |
|---|:---:|:---:|:---:|
| **Concurrent Virtual Users** | **10** | **50** | **100** |
| **Total Requests** | 200 | 500 | 1000 |
| **Duration (seconds)** | 0.729 s | 2.308 s | 4.904 s |
| **Throughput (req/sec)** | **274.19 req/s** | **216.59 req/s** | **203.91 req/s** |
| **Average Latency (ms)** | **31.67 ms** | **87.87 ms** | **272.54 ms** |
| **Median Latency (ms)** | 30.66 ms | 67.71 ms | 250.70 ms |
| **P95 Latency (ms)** | **49.55 ms** | **195.85 ms** | **525.58 ms** |
| **P99 Latency (ms)** | 64.58 ms | 248.03 ms | 668.34 ms |
| **Success Rate (%)** | **100.0%** (200 / 200) | **100.0%** (500 / 500) | **100.0%** (1000 / 1000) |
| **Server Error Rate (5xx)** | **0.00%** | **0.00%** | **0.00%** |

*Raw Telemetry*: `results/raw/workload_results.json`  
*Generated Plot*: `results/figures/scalability_workload_tiers.png`

---

## 2. Baseline vs. Optimized Search Architecture Comparison

A comparative evaluation measured unindexed regex search against indexed, cached catalogue search under identical hardware conditions (20 concurrent virtual users, 150 requests):

| Metric | Baseline Architecture (Unindexed Regex) | Optimized Architecture (Indexed & Cached) | Percentage Improvement |
|---|:---:|:---:|:---:|
| **Average Latency** | 76.17 ms | **69.60 ms** | **-8.6%** (Speedup: 1.09x) |
| **Median Latency** | 73.58 ms | **67.29 ms** | **-8.5%** |
| **P95 Tail Latency** | 125.99 ms | **109.67 ms** | **-13.0%** |
| **Throughput** | 210.75 req/s | **231.19 req/s** | **+9.7%** |
| **Error Rate** | 0.00% | 0.00% | Neutral |

*Raw Telemetry*: `results/raw/search_comparison.json`  
*Generated Plot*: `results/figures/baseline_vs_optimized_search.png`

---

## 3. Component-Specific Performance Metrics

### 3.1 Rate Limiter Rejection Rate (Component #1)
- **Workload**: 25 concurrent threads submitting 50 rapid login requests to intentionally exceed the capacity=10 token bucket.
- **Accepted Requests**: 10 requests (20.0%).
- **Throttled Requests**: 40 requests received `HTTP 429 Too Many Requests` (rejection rate: **80.0%**).
- *Raw Telemetry*: `results/raw/ratelimiter_evaluation.json`
- *Generated Plot*: `results/figures/ratelimiter_rejection_pie.png`

### 3.2 Consistent Hashing Churn on Ring Expansion (Component #3)
- **Simulation**: 2,000 keys mapped across physical server nodes with 160 virtual nodes each. Ring expanded from 3 to 4 nodes.
- **Consistent Hashing Keys Remapped**: **28.3%** (566 / 2,000 keys) — close to theoretical optimal $\frac{1}{N+1} = 25.0\%$.
- **Modulo Hashing Keys Remapped**: **75.1%** (1,502 / 2,000 keys) — matches theoretical $\frac{N}{N+1} = 75.0\%$.
- *Conclusion*: Consistent hashing reduces cache invalidation and database storm by **62.3%**.
- *Raw Telemetry*: `results/raw/consistent_hashing.json`
- *Generated Plot*: `results/figures/consistent_hashing_churn.png`

### 3.3 Autocomplete Latency & Precision@5 (Component #8)
- **Vocabulary**: 13 indexed books, authors, and categories.
- **Mean Traversal Latency**: **$< 1.0$ ms** (0.000 ms measured wall-clock).
- **Mean Precision@5**: **0.57** across evaluated prefix vocabulary.
- *Raw Telemetry*: `results/raw/autocomplete_evaluation.json`

### 3.4 Web Crawler Ingestion Throughput (Component #7)
- **Source**: Mock external library publisher API (`http://localhost:8080/mock-external-library/books`).
- **Records Processed**: 7 records fetched, 7 parsed, 2 imported (new titles), 5 deduplicated (existing ISBNs).
- **Execution Duration**: 123 ms.
- **Measured Throughput**: **56.91 books/second**.
- *Raw Telemetry*: `results/raw/crawler_jobs.json`

### 3.5 URL Shortener Redirection Latency (Component #6)
- **Sample Code**: Base62 short URL for catalogue title.
- **50 Sequential Redirection Requests**: Mean latency **25.19 ms**, P95 latency **30.31 ms**.
- *Raw Telemetry*: `results/raw/url_shortener.json`
