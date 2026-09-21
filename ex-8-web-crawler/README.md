# Web Crawler for Automated Web Content Discovery — Lab Exercise 8

**UCS3513 System Design Laboratory** — Sri Sivasubramaniya Nadar College of Engineering

A distributed, asynchronous Web Crawler developed with **Spring Boot** and **Redis**. It implements queue-based frontier scheduling, atomic visited-URL set deduplication, HTML hyperlink discovery via **Jsoup**, canonical URL normalization, and sub-millisecond page metadata caching.

---

## What the Assignment Requires (and Where It Is)

| Assignment Requirement | Implementation Location |
| :--- | :--- |
| **1. Create seed dataset & sample web resources** | `src/main/resources/dataset/seed_urls.csv`, `src/main/resources/mock-web/*.html` |
| **2. Spring Boot crawling application** | `src/main/java/com/crawler/` |
| **3. Implement URL queue for pending URLs** | `service/UrlFrontierQueue.java` (Redis List `RPUSH`/`LPOP`) |
| **4. Retrieve web pages from crawling queue** | `service/HtmlParserService.java`, `service/WebCrawlerEngine.java` |
| **5. Extract hyperlinks & add new URLs to queue** | `HtmlParserService.fetchAndParse()`, `WebCrawlerEngine.processTask()` |
| **6. Visited-URL set avoiding duplicate crawling** | `service/VisitedUrlService.java` (Redis Set `SADD`/`SISMEMBER`) |
| **7. Validation for valid & permitted URLs** | `service/UrlValidator.java` (Scheme, Domain, Non-HTML filters) |
| **8. Store crawling information & status** | `model/CrawlStats.java`, `model/PageMetadata.java`, `service/PageCacheService.java` |
| **9. Integrate Redis for queue & state caching** | `config/RedisConfig.java`, `service/PageCacheService.java` |
| **10. Use Redis visited info to avoid duplicates** | `VisitedUrlService.markVisitedIfAbsent()` |
| **11. Expose REST API and test using Postman** | `controller/CrawlerController.java`, `scripts/WebCrawler_API.postman_collection.json` |
| **12. Deploy application and Redis using Docker** | `Dockerfile`, `docker-compose.yml` |
| **13. JMeter concurrent crawling performance tests** | `scripts/WebCrawler_Load_Test.jmx`, `scripts/WebCrawler_Concurrent_Batch.jmx` |
| **14. Generate Lab Report Word & PDF Document** | `scripts/gen_report.py` -> `SD_A8.docx` and `SD_A8_Web_Crawler.pdf` |

---

## Quick Start

### Automated Orchestration (Recommended)
Build, run, execute live benchmarks, and generate the lab report in a single command:

```powershell
# Windows (PowerShell)
.\run_lab.ps1

# Linux / macOS / Git Bash
./run_lab.sh
```

### Manual Execution

1. **Start Redis (Docker)**:
   ```bash
   docker compose up -d redis
   ```
2. **Build and Run Spring Boot Application**:
   ```bash
   mvn spring-boot:run
   ```
3. **Execute Demos and Benchmarks**:
   ```bash
   python scripts/load_test.py demo         # Step-by-step crawl & deduplication verification
   python scripts/load_test.py benchmark    # Latency comparison: Fresh fetch vs Redis cache
   python scripts/load_test.py concurrent   # Multi-threaded stress load test
   python scripts/gen_report.py             # Generate SD_A8.docx and SD_A8_Web_Crawler.pdf
   ```

* **Interactive Web Dashboard**: [http://localhost:8080/](http://localhost:8080/)
* **Swagger OpenAPI Documentation**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **Sample Mock Web Portal**: [http://localhost:8080/mock-web/index.html](http://localhost:8080/mock-web/index.html)

---

## REST API Endpoints

### 1. `POST /api/crawler/start`
Starts an asynchronous multi-threaded crawling job.
* **Request Body**:
  ```json
  {
    "seedUrls": ["http://localhost:8080/mock-web/index.html"],
    "maxDepth": 3,
    "maxPages": 50,
    "workers": 4,
    "politenessDelayMs": 50
  }
  ```
* **Response**: Returns initialized `CrawlStats` with `jobId` and `status: "RUNNING"`.

### 2. `POST /api/crawler/crawl-single?url=...`
Synchronously fetches a single URL. Checks Redis page cache first.
* On **Cache Hit**: Returns cached document metadata in **0.44 ms** (`source: "REDIS_CACHE"`).
* On **Cache Miss**: Fetches via Jsoup, parses DOM, caches metadata in Redis, and returns (`source: "LIVE_FETCH"`).

### 3. `GET /api/crawler/status`
Returns real-time crawling metrics:
```json
{
  "jobId": "job-a1b2c3d4",
  "status": "COMPLETED",
  "totalDiscovered": 42,
  "totalCrawled": 18,
  "queueSize": 0,
  "visitedCount": 18,
  "duplicateUrlsAvoided": 34,
  "elapsedTimeMs": 1250,
  "throughputPagesPerSec": 14.4,
  "storageMode": "REDIS"
}
```

### 4. `GET /api/crawler/pages`
Returns list of all crawled pages with HTTP status codes, titles, outbound links count, and crawl latencies.

### 5. `GET /api/crawler/visited`
Returns all distinct URLs stored in the Redis visited set (`crawl:visited`).

### 6. `GET /api/crawler/queue`
Peeks pending crawl tasks currently scheduled in the Redis URL frontier list (`crawl:queue`).

### 7. `POST /api/crawler/reset`
Flushes the Redis frontier list, visited set, content search index, and page cache.

### 8. `GET /api/crawler/search?q={query}&sortBy={COMPOSITE|PAGERANK|BM25|WORDS|NEWEST}&category={cat}`
Performs multi-field inverted index search across discovered web content:
* **Okapi BM25 Ranking**: Calibrates term frequency saturation ($k_1=1.2$), document length normalization ($b=0.75$), and inverse document frequency ($\text{IDF}$).
* **Porter Stemmer**: Normalizes inflected terms (e.g. `computing` -> `comput`, matching `computer`, `computational`).
* **Field Weights**: Boosts document titles ($5.0\times$), headings ($3.0\times$), meta descriptions/keywords ($2.5\times$), and body text ($1.0\times$).
* **Composite Ranking**: Blends BM25 textual relevance ($65\%$) with Google PageRank authority ($35\%$).
* **"Fetch Everything" Aggregation**: Returns total occurrences across the entire corpus, category distribution breakdowns, and related entity co-occurrences.

### 9. `GET /api/crawler/autocomplete?prefix={term}&limit=8`
Instant prefix suggestions from the crawled vocabulary index with corpus frequency counts in $< 1 \text{ ms}$.

### 10. `GET /api/crawler/pagerank`
Returns Google PageRank authority distribution and leaderboard across all crawled nodes ($d=0.85$, power iteration convergence).

### 11. `POST /api/crawler/pagerank/recompute`
Forces re-execution of PageRank power iteration across the link graph.

### 12. `GET /api/crawler/topics`
Returns corpus category clustering and top discovered keyword entities with document counts.

### 13. `GET /api/crawler/export?format=json|csv`
Exports all discovered web page content, headings, word counts, and metadata as structured JSON or CSV for downstream data pipelines or search indexers.

---

## Key System Design Decisions

1. **Redis Frontier Queue (`crawl:queue`)**:
   - Implemented via Redis Lists using `RPUSH` to enqueue newly discovered links and `LPOP` to dispatch tasks to worker threads.
   - Preserves FIFO ordering for Breadth-First Search (BFS) graph exploration.
2. **Atomic Visited Set Deduplication (`crawl:visited`)**:
   - Redis Sets (`SADD`) return `1` if an element was absent and `0` if it was already recorded.
   - Provides thread-safe, lock-free deduplication across concurrent crawler workers.
   - Tested on cyclic graphs (`cyclic-a.html` <-> `cyclic-b.html`) where it successfully prevents infinite loops.
3. **Automated Robots.txt Compliance (RFC 9309)**:
   - Fetches and caches `/robots.txt` per host; implements longest-match prefix precedence between `Allow` and `Disallow` rules.
   - Respects `<meta name="robots" content="nofollow">` tags in parsed HTML documents.
4. **Autonomous XML Sitemap Discovery (sitemaps.org)**:
   - Discovers seed pages via `/sitemap.xml` and robots.txt `Sitemap:` directives.
   - Recursively parses standard URLsets and hierarchical sitemap indexes.
5. **Content-Seen (Near-Duplicate) Deduplication via SHA-256**:
   - Computes SHA-256 hash of normalized main body text after stripping structural boilerplate (`<nav>`, `<header>`, `<footer>`, `<aside>`, `<script>`).
   - Uses Redis `SETNX crawl:content:hash:<sha256> <url>` to detect cloned or syndicated content across different URLs.
6. **Information Retrieval & BM25 Scoring**:
   - Multi-field inverted index with Porter morphological stemming.
   - Okapi BM25 scoring with non-linear term saturation ($k_1=1.2, b=0.75$) and dynamic snippet highlighting.
7. **Google PageRank Link Authority Algorithm**:
   - Computes authority using power iteration ($d=0.85, \epsilon=10^{-4}$), redistributes dangling node mass, and normalizes scores to $0-100$.
   - Graph visualizer dynamically scales node radii according to PageRank authority.
8. **Canonicalization & Boundary Validation**:
   - Strips URL fragments (`#section`) and tracking parameters (`utm_*`, `ref`, session IDs).
   - Normalizes schemes, ports, and hosts to lowercase, and sorts query parameters alphabetically.
   - Filters binary assets (`.pdf`, `.png`, `.zip`, etc.) and non-HTTP protocols (`mailto:`, `javascript:`, `ftp:`).
   - Enforces domain boundaries (staying on `localhost` or authorized domains).
9. **Resiliency & In-Memory Fallback**:
   - If Redis is unavailable or down, the application automatically falls back to thread-safe Java collections (`ConcurrentLinkedQueue`, `ConcurrentHashMap.newKeySet()`, `ConcurrentHashMap`), ensuring 100% operational uptime.

---

## Performance Summary

* **Cache Speedup**: Redis page caching delivers an average **29.1x speedup** over live HTML network fetches (12.8 ms down to 0.44 ms).
* **Throughput**: Scales up to **2,890 req/sec** under 100 concurrent JMeter worker threads with zero error rate.
* **Deduplication Ratio**: Averts over 65% redundant network downloads on interconnected web graphs.
* **Search Execution**: Sub-millisecond full-text retrieval across indexed content (< 2 ms).
* **PageRank Convergence**: Power iteration converges in under 20 iterations for complex cyclic link topologies.

