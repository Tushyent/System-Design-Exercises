# Distributed Web Crawler & Automated Web Content Discovery Engine
## Complete Technical Documentation, Architecture Guide & Presentation Manual
**Course:** UCS3513 System Design Laboratory — Lab Exercise 8  
**Department:** Computer Science and Engineering, Sri Sivasubramaniya Nadar College of Engineering  
**Technology Stack:** Java 21 LTS, Spring Boot 3.3, Redis, Jsoup, Okapi BM25, Google PageRank, HTML5 Canvas

---

# Table of Contents
1. [Executive Summary & Academic Context](#1-executive-summary--academic-context)
2. [Problem Statement & Core Requirements](#2-problem-statement--core-requirements)
3. [Theoretical Foundations & System Design Principles](#3-theoretical-foundations--system-design-principles)
   - [Graph Theory of the World Wide Web](#graph-theory-of-the-world-wide-web)
   - [Why Breadth-First Search (BFS) vs Depth-First Search (DFS)](#why-breadth-first-search-bfs-vs-depth-first-search-dfs)
   - [The Complete Crawler Lifecycle](#the-complete-crawler-lifecycle)
4. [System Architecture & Core Components](#4-system-architecture--core-components)
   - [Architecture Diagram](#architecture-diagram)
   - [Spring Boot Application Layer](#spring-boot-application-layer)
   - [Multi-Threaded Concurrency Model](#multi-threaded-concurrency-model)
   - [Redis Storage & Distributed State Tier](#redis-storage--distributed-state-tier)
5. [Content Discovery, HTML Parsing & Cleaning](#5-content-discovery-html-parsing--cleaning)
   - [Jsoup DOM Engine](#jsoup-dom-engine)
   - [The 5-Stage Content Cleaning Pipeline](#the-5-stage-content-cleaning-pipeline)
   - [Extracting and Taking Down Raw HTML vs Clean Text](#extracting-and-taking-down-raw-html-vs-clean-text)
6. [Deduplication, Boundaries & Politeness](#6-deduplication-boundaries--politeness)
   - [URL Canonicalization](#url-canonicalization)
   - [Loop Avoidance via Visited Sets](#loop-avoidance-via-visited-sets)
   - [SHA-256 Checksum Content Deduplication (Clone Detection)](#sha-256-checksum-content-deduplication-clone-detection)
   - [Robots.txt & Sitemap Parsing](#robotstxt--sitemap-parsing)
7. [Information Retrieval, Indexing & Ranking Algorithms](#7-information-retrieval-indexing--ranking-algorithms)
   - [Inverted Index Architecture](#inverted-index-architecture)
   - [Okapi BM25 Relevance Scoring (Mathematical Breakdown)](#okapi-bm25-relevance-scoring-mathematical-breakdown)
   - [Google PageRank Algorithm (Power Iteration Breakdown)](#google-pagerank-algorithm-power-iteration-breakdown)
   - [Composite Ranking Formula](#composite-ranking-formula)
   - [Trie Prefix Autocomplete Data Structure](#trie-prefix-autocomplete-data-structure)
8. [Data Persistence & Discovered Website Taking-Down](#8-data-persistence--discovered-website-taking-down)
   - [Physical Disk Archive Directory Structure](#physical-disk-archive-directory-structure)
   - [All Discovered Websites Registry & Graph Provenance](#all-discovered-websites-registry--graph-provenance)
9. [REST API Specification](#9-rest-api-specification)
10. [Frontend Architecture (Guided Stepper & Dark Console)](#10-frontend-architecture-guided-stepper--dark-console)
11. [Viva Presentation Guide: Step-by-Step Script for Mam](#11-viva-presentation-guide-step-by-step-script-for-mam)
12. [Comprehensive Viva Examination Q&A (15 Core Questions)](#12-comprehensive-viva-examination-qa-15-core-questions)

---

# 1. Executive Summary & Academic Context

In the modern internet era, the World Wide Web consists of billions of unstructured, dynamically linked hypermedia documents. To organize, search, and extract value from this ocean of data, search engines, threat-intelligence units, and e-commerce aggregators employ **Automated Web Crawlers** (also known as web spiders or bots).

In **Lab Exercise 8 of the UCS3513 System Design Laboratory**, we designed and developed a high-performance, asynchronous, multi-threaded Web Crawler and Content Discovery system. 

Built using **Java 21 LTS** and **Spring Boot 3.3.0**, the system coordinates a pool of worker threads that traverse the web using **Breadth-First Search (BFS)**, leverages **Redis** for distributed FIFO queues and $O(1)$ atomic deduplication, cleans and parses HTML documents using **Jsoup**, archives all discovered websites and their raw HTML/clean text to disk, and indexes the text corpus into a full-text search engine powered by **Okapi BM25** and **Google PageRank**.

---

# 2. Problem Statement & Core Requirements

### Official Lab Exercise 8 Objectives:
1. **Automated Discovery:** Start from user-defined Seed URLs, fetch pages over HTTP, discover all outbound links (`<a href="...">`), and recursively discover new pages.
2. **Frontier Queue Management:** Implement a scalable, thread-safe URL queue to manage URLs waiting to be crawled.
3. **Loop & Duplicate Prevention:** Maintain an atomic visited-URL set ensuring no URL is fetched more than once, completely preventing infinite cyclic loops.
4. **Redis Integration:** Use Redis for distributed URL frontier queuing, atomic visited-state checking, and sub-millisecond document caching.
5. **Content Extraction & Archiving:** Take down and permanently archive all discovered websites and their full contents (both clean readable text and original raw HTML markup) to disk.
6. **Politeness & Safety:** Parse and strictly enforce `robots.txt` exclusion rules, sitemap discovery, and configurable politeness request delays.
7. **Search & Link Authority:** Index crawled text into an Inverted Index, implement Okapi BM25 relevance scoring, calculate Google PageRank authority, and support Trie autocomplete.
8. **Evaluation:** Provide REST APIs, test performance under concurrent multi-threaded workloads, and provide an interactive evaluation dashboard.

---

# 3. Theoretical Foundations & System Design Principles

## Graph Theory of the World Wide Web
The web is formally modeled as a **directed cyclic multigraph**:
$$G = (V, E)$$
- **Vertices ($V$):** Webpages uniquely identified by their canonical Uniform Resource Locator (URL).
- **Edges ($E$):** Hyperlinks connecting page $u$ to page $v$ (`<a href="v">`).

Unlike trees or directed acyclic graphs (DAGs), the web contains:
- **Cycles:** Page A links to Page B, and Page B links back to Page A.
- **Spider Traps:** Dynamically generated infinite URLs (e.g. calendars `cal.php?month=1`, `cal.php?month=2`...).
- **Dead Ends:** Pages with no outbound links.

## Why Breadth-First Search (BFS) vs Depth-First Search (DFS)

Choosing the correct graph traversal strategy is the single most critical architectural decision in crawler design.

```
            [Seed Page (Depth 0)]
                 /         \
       [Tech Hub (D=1)]   [Sports (D=1)]
          /        \          /       \
      [AI (D=2)] [Cloud]   [Cricket] [Football]
```

### Why NOT Depth-First Search (DFS)?
1. **Spider Traps:** If DFS encounters an infinite dynamic link generator, it will drill infinitely deep down a single path, filling up call stacks and never escaping.
2. **Server Politeness Violation:** DFS follows links within the same website continuously, bombarding a single host with hundreds of requests per second, triggering IP bans and DoS alerts.
3. **Peripheral / Low-Value Content:** Deep pages (depth 10+) are usually obscure user comments, old archives, or auto-generated filters.
4. **Call Stack Overhead:** Recursive DFS risks `StackOverflowError` on large crawls.

### Why Breadth-First Search (BFS)?
1. **Layer-by-Layer Priority:** BFS visits Depth 0 (Seeds), then Depth 1 (Direct links), then Depth 2, systematically expanding outwards.
2. **Strict Depth Bounding:** It naturally enforces `maxDepth`. When `maxDepth = 3`, exploration halts cleanly.
3. **Domain Diversity & Politeness:** BFS alternates between different domains and sections, distributing server load fairly.
4. **Authoritative Content First:** The most reputable, high-PageRank pages (homepages, category portals) are located within 1 to 3 hops from the root.
5. **FIFO Queue Alignment:** BFS maps cleanly to an asynchronous First-In, First-Out (FIFO) queue (`RPUSH` to enqueue, `LPOP` to dequeue).

## The Complete Crawler Lifecycle

Every URL processed by the crawler passes through this strict 8-stage pipeline:

```
[ Seed URL ]
     │
     ▼
1. Canonical Normalization (Trim, lowercase host, strip #anchors)
     │
     ▼
2. Robots.txt & Sitemap Check (Obey Disallow paths, harvest XML sitemaps)
     │
     ▼
3. Frontier Queue Insertion (Redis List: RPUSH crawler:frontier:queue)
     │
     ▼
4. Worker Thread Dequeue & Visited Check (Redis Set: SADD crawler:visited:set)
     │ (If already visited ──> Discard immediately)
     ▼
5. HTTP Network Fetch & Jsoup Parsing (Timeout = 5000ms, Extract DOM)
     │
     ▼
6. Content Extraction & Deduplication (Strip scripts, SHA-256 body hash)
     │ (If duplicate hash ──> Flag CLONE, prevent redundant search indexing)
     ▼
7. Permanent Disk Archiving (Save .html, .txt, and .json to /crawled_data/pages/)
     │
     ▼
8. Hyperlink Extraction & Re-enqueueing (Extract <a href>, push new links to FIFO)
```

---

# 4. System Architecture & Core Components

## Architecture Diagram

```
+=======================================================================================+
|                   DISTRIBUTED WEB CRAWLER & CONTENT DISCOVERY PIPELINE                |
+=======================================================================================+

                             [ Client Browser / Postman / JMeter ]
                                                │
                                                ▼ HTTP REST API
                            ┌───────────────────────────────────────┐
                            │    CrawlerController.java (Port 8080) │
                            └───────────────────────────────────────┘
                                                │
                                                ▼
                            ┌───────────────────────────────────────┐
                            │        WebCrawlerEngine.java          │
                            │   (ThreadPoolTaskExecutor: 4-16)      │
                            └───────────────────────────────────────┘
                                       │                │
             ┌─────────────────────────┘                └────────────────────────┐
             ▼                                                                   ▼
┌─────────────────────────┐                                         ┌─────────────────────────┐
│     STATE STORAGE       │                                         │    PROCESSING SERVICES  │
│  (Redis / In-Memory)    │                                         │                         │
├─────────────────────────┤                                         ├─────────────────────────┤
│ • UrlFrontierQueue      │ ◄── FIFO Queue (RPUSH / LPOP)           │ • UrlValidator          │
│ • VisitedUrlService     │ ◄── Visited Set (SADD / SISMEMBER)      │ • RobotsTxtService      │
│ • PageCacheService      │ ◄── Page Cache (SETEX / GET)            │ • HtmlParserService     │
└─────────────────────────┘                                         │ • CrawledDataStorage    │
             │                                                      │ • InvertedIndexService  │
             ▼                                                      │ • SearchRankingService  │
┌─────────────────────────┐                                         │ • PageRankService       │
│   PERSISTENT STORAGE    │                                         │ • TrieAutocomplete     │
│    (/crawled_data)      │                                         └─────────────────────────┘
├─────────────────────────┤                                                      │
│ • /pages/{hash}.html    │                                                      ▼
│ • /pages/{hash}.txt     │                                         ┌─────────────────────────┐
│ • /pages/{hash}.json    │                                         │   INTERACTIVE UI        │
│ • discovered_websites   │                                         │    (index.html)         │
│ • crawled_summary.csv   │                                         │  • Guided Stepper       │
└─────────────────────────┘                                         │  • Dark Theme Console   │
                                                                    └─────────────────────────┘
```

## Spring Boot Application Layer
- **`WebCrawlerApplication.java`:** Bootstrap class with `@SpringBootApplication` and `@EnableAsync`.
- **`CrawlerController.java`:** Exposes 10+ REST API endpoints managing crawl jobs, query execution, PageRank calculation, and raw file downloads.
- **`WebCrawlerEngine.java`:** Central orchestrator managing worker lifecycle, thread pools, job statistics (`CrawlStats`), and BFS termination criteria.

## Multi-Threaded Concurrency Model
Single-threaded web crawling is fundamentally limited by network latency. If a web server takes 200 ms to respond, a single thread can only process 5 pages per second.

In our system:
- **`ThreadPoolTaskExecutor`:** Configured with a core pool of 4 threads and a maximum of 16 threads.
- **Non-blocking Concurrency:** While `CrawlerWorker-1` is waiting for an HTTP socket response from page A, `CrawlerWorker-2` is parsing the DOM of page B, `CrawlerWorker-3` is writing page C to disk, and `CrawlerWorker-4` is executing PageRank iterations.
- **Thread Safety:** All shared resources use lock-free or atomic primitives (`AtomicInteger`, `ConcurrentLinkedQueue`, `ConcurrentHashMap`, Redis atomic operations).

## Redis Storage & Distributed State Tier
Redis provides sub-millisecond, in-memory distributed data structures:

| Component | Redis Data Structure | Redis Key Pattern | Redis Commands Used | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Frontier Queue** | List | `crawler:frontier:queue` | `RPUSH`, `LPOP`, `LLEN` | Distributed FIFO queue for pending crawl tasks. |
| **Visited Set** | Set | `crawler:visited:set` | `SADD`, `SISMEMBER`, `SCARD` | $O(1)$ atomic membership check to prevent duplicate visits and cycles. |
| **Page Cache** | Hash / String | `crawler:cache:pages` | `SETEX`, `GET`, `DEL` | Sub-millisecond retrieval of pre-parsed document metadata. |

### Graceful In-Memory Fallback:
If Redis is not installed or Docker is offline, the system detects `redisAvailable = false` and seamlessly falls back to thread-safe Java collections:
- Frontier: `ConcurrentLinkedQueue<CrawlTask>`
- Visited Set: `ConcurrentHashMap.newKeySet()`
- Page Cache: `ConcurrentHashMap<String, PageMetadata>`

---

# 5. Content Discovery, HTML Parsing & Cleaning

## Jsoup DOM Engine
We utilize **Jsoup 1.17.2**, a Java HTML parser implementing the WHATWG HTML5 specification. It parses raw network byte streams into a navigable Document Object Model (DOM) tree.

## The 5-Stage Content Cleaning Pipeline
In [`HtmlParserService.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/HtmlParserService.java), raw markup is converted into clean text:

1. **DOM Tree Construction:**
   ```java
   Document doc = Jsoup.parse(rawHtml, baseUri);
   ```
2. **Noise Tag Removal:**
   Non-content elements (scripts, styling, tracking frames, vector graphics) are purged:
   ```java
   doc.select("script, style, noscript, svg, iframe").remove();
   ```
3. **Structured Heading Extraction:**
   Document outline is captured from $H1, H2, H3$ tags:
   ```java
   List<String> headings = doc.select("h1, h2, h3").eachText();
   ```
4. **Clean Text Extraction & Entity Decoding:**
   Extracts pure human-readable text, converts HTML entities (`&amp;` &rarr; `&`, `&quot;` &rarr; `"`), and collapses irregular whitespaces:
   ```java
   String cleanText = doc.body().text();
   ```
5. **Tokenization & Stop-Word Filtering:**
   Text is lowercased, stripped of non-alphanumeric punctuation using regex `[^\w\s]`, and filtered against 40+ common English stop words (`the`, `is`, `at`, `which`, `on`...) to prepare tokens for the inverted index.

## Extracting and Taking Down Raw HTML vs Clean Text
To satisfy the lab requirement of **taking down all website contents**, we preserve both forms:
- **Clean Text (`.txt`):** Stripped of all HTML, used for natural language search, reading time calculation, word counting, and topic extraction.
- **Raw HTML (`.html`):** The exact source code downloaded from the server, allowing offline reproduction and visual mirroring.

---

# 6. Deduplication, Boundaries & Politeness

## URL Canonicalization
Web servers often serve the identical page under syntactically different URLs:
- `http://example.com/index.html`
- `HTTP://EXAMPLE.COM/index.html`
- `http://example.com/index.html#section-2`
- `http://example.com/index.html?`

In [`UrlValidator.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/UrlValidator.java), we apply canonical normalization:
1. Scheme and host are converted to lowercase.
2. Port 80 (HTTP) and 443 (HTTPS) are stripped if explicitly declared.
3. Fragment anchors (`#...`) are removed.
4. Trailing slashes are standardized.

## Loop Avoidance via Visited Sets
When Worker 1 encounters a URL, it queries `VisitedUrlService`:
```java
boolean isNew = visitedUrlService.markVisitedIfAbsent(canonicalUrl);
if (!isNew) {
    stats.incrementDuplicateUrlsAvoided();
    return; // Discard: Loop prevented
}
```
In Redis, this executes `SADD`. Because `SADD` is atomic, even if 4 worker threads encounter the same URL at the exact same microsecond, exactly one worker receives return value `1`, while the other 3 receive `0` and discard the task.

## SHA-256 Checksum Content Deduplication (Clone Detection)
URL deduplication only prevents visiting the *same address*. It fails when a website publishes the identical article under different URLs (e.g. `/duplicate-article-1.html` and `/duplicate-article-2.html`).

In [`WebCrawlerEngine.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/WebCrawlerEngine.java):
1. We compute a cryptographic SHA-256 hash over the normalized clean body text:
   ```java
   String contentHash = DigestUtils.sha256Hex(cleanText);
   ```
2. We check an in-memory hash registry (`contentHashToUrlMap`).
3. If the hash exists, the page is flagged as a **`CLONE`**, the original URL is recorded, and redundant search indexing is skipped.

## Robots.txt & Sitemap Parsing
- **Robots.txt ([`RobotsTxtService.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/RobotsTxtService.java)):**
  Before visiting a host, the crawler retrieves `/robots.txt`. It parses `User-agent:` and `Disallow:` directives. If an admin disallows `/mock-web/private-admin.html`, the crawler skips it, logging a `ROBOTS_DISALLOW` event.
- **Sitemaps:** The crawler attempts to fetch `/sitemap.xml`. It parses all `<loc>` tags, adding hidden or deep URLs directly into the frontier queue.
- **Politeness Delay:** Configurable delay (default 250 ms) inserted between requests to ensure target servers are never overwhelmed.

---

# 7. Information Retrieval, Indexing & Ranking Algorithms

## Inverted Index Architecture
Searching through documents sequentially (Forward Index: Document &rarr; Words) has time complexity $O(N \times L)$, which is unacceptable at web scale.

In [`InvertedIndexService.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/InvertedIndexService.java), we construct an **Inverted Index** (Word &rarr; Postings List):

```
Term: "quantum"   ──> [ {DocID: 14, Freq: 5, Positions: [1, 12, 45]}, 
                        {DocID: 15, Freq: 3, Positions: [4, 18]} ]
Term: "computing" ──> [ {DocID: 14, Freq: 4}, {DocID: 6, Freq: 2} ]
```
Query lookup takes $O(1)$ hash time to locate the postings list, followed by fast intersection of document sets.

## Okapi BM25 Relevance Scoring (Mathematical Breakdown)
Okapi BM25 is the industry-standard probabilistic ranking function used by **Elasticsearch** and **Apache Lucene**.

$$\text{BM25}(D, Q) = \sum_{t \in Q} \text{IDF}(t) \cdot \frac{f(t, D) \cdot (k_1 + 1)}{f(t, D) + k_1 \cdot \left(1 - b + b \cdot \frac{|D|}{\text{avgdl}}\right)}$$

### Where:
- $f(t, D)$ = Term Frequency of word $t$ in document $D$.
- $|D|$ = Document length (total word count).
- $\text{avgdl}$ = Average document length across the entire crawled corpus.
- $k_1 = 1.5$ = Controls term frequency saturation.
- $b = 0.75$ = Controls document length normalization.

### Two Crucial BM25 Principles:
1. **Term Frequency Saturation:** Mentioning a keyword 20 times does not make a page 20 times more relevant than mentioning it 5 times. BM25 curves TF asymptotically.
2. **Document Length Normalization:** A 50-word news snippet matching the query twice is much more relevant than a 50,000-word book matching the query twice.

$$\text{IDF}(t) = \ln\left(1 + \frac{N - n(t) + 0.5}{n(t) + 0.5}\right)$$
- $N$ = Total documents in corpus.
- $n(t)$ = Number of documents containing term $t$.

## Google PageRank Algorithm (Power Iteration Breakdown)
PageRank measures structural link authority independent of content keywords.

- **Random Surfer Model:** A web surfer clicks random links with probability $d = 0.85$ (damping factor), and jumps to a random page on the internet with probability $(1 - d) = 0.15$.

$$\text{PR}(u) = \frac{1 - d}{N} + d \sum_{v \in B_u} \frac{\text{PR}(v)}{L(v)}$$

- $B_u$ = Set of all pages linking into page $u$.
- $L(v)$ = Total outbound links on referring page $v$.
- $N$ = Total pages in the link graph.

### Implementation in [`PageRankService.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/PageRankService.java):
We solve the linear system using **Power Iteration**:
1. Initialize $\text{PR}_0(u) = \frac{1}{N}$ for all nodes.
2. Distribute probability mass across outgoing edges in successive rounds.
3. Terminate when convergence is reached: $\sum |\text{PR}_k - \text{PR}_{k-1}| < 10^{-4}$ (converges in 22 iterations).
4. Scale raw PageRank scores into an intuitive 0–100 authority range.

## Composite Ranking Formula
In [`SearchRankingService.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/SearchRankingService.java), the final ranking merges lexical matching with topological authority:

$$\text{Final Score} = 0.6 \times \text{BM25 Relevance} + 0.4 \times \text{PageRank Authority}$$

This prevents spam pages stuffed with repeated keywords from outranking authoritative pages like the root portal.

## Trie Prefix Autocomplete Data Structure
In [`TrieAutocompleteService.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/service/TrieAutocompleteService.java), vocabulary words are inserted into a **Trie (Prefix Tree)**:

```
        (root)
       /      \
     [q]      [n]
      │        │
     [u]      [e]
      │        │
     [a]      [u]
      │        │
     [n]      [r]
      │        │
   [quantum]  [neural]
```
Prefix lookup takes $O(L)$ time (where $L$ is prefix length, e.g. 3 keystrokes), returning top matching search suggestions instantly.

---

# 8. Data Persistence & Discovered Website Taking-Down

## Physical Disk Archive Directory Structure
All crawled data is permanently stored under [`crawled_data/`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/crawled_data/):

```
crawled_data/
├── discovered_urls.txt         <-- Plain text list of all discovered URLs
├── discovered_websites.json     <-- Structured JSON registry of all discovered websites
├── all_crawled_content.json    <-- Consolidated dataset of all pages
├── crawled_summary.csv         <-- Tabular CSV summary for analysis
└── pages/                      <-- Individual per-page archives
    ├── {url_hash}.html         <-- Full raw original HTML source code
    ├── {url_hash}.txt          <-- Clean readable body text
    └── {url_hash}.json         <-- Metadata (title, word count, headings, links)
```

## All Discovered Websites Registry & Graph Provenance
Every discovered URL is tracked via [`DiscoveredWebsite.java`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/java/com/crawler/model/DiscoveredWebsite.java), recording:
1. **URL:** The target web address.
2. **Parent URL:** The referrer page where this URL was first discovered (graph provenance).
3. **Depth:** Hop distance from the seed URL.
4. **Crawl State:** `CRAWLED`, `IN_QUEUE`, or `ROBOTS_DISALLOW`.
5. **Status Code:** HTTP 200, 403, 404.
6. **Word Count & Category:** Document classification.

---

# 9. REST API Specification

| HTTP Method | Endpoint | Description | Sample Parameters / Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/crawler/start` | Starts an asynchronous BFS crawl job | `{"seedUrls":["http://localhost:8080/mock-web/index.html"],"maxDepth":3,"maxPages":50,"workers":4,"politenessDelayMs":250}` |
| `POST` | `/api/crawler/stop` | Stops the running crawler | None |
| `POST` | `/api/crawler/reset` | Clears all queues, visited sets, disk archives, and index | None |
| `GET` | `/api/crawler/status` | Returns real-time metrics (throughput, queue, visited, etc.) | None |
| `GET` | `/api/crawler/pages` | Returns list of all crawled pages with metadata | None |
| `GET` | `/api/crawler/discovered` | Returns complete registry of all discovered URLs | None |
| `GET` | `/api/crawler/search` | Queries corpus using Okapi BM25 and PageRank | `?q=quantum&sortBy=COMPOSITE&category=Quantum Computing` |
| `GET` | `/api/crawler/autocomplete` | Returns Trie prefix suggestions | `?prefix=qua&limit=7` |
| `GET` | `/api/crawler/pagerank` | Returns PageRank authority leaderboard | None |
| `POST` | `/api/crawler/pagerank/recompute` | Forces power iteration recalculation | None |
| `POST` | `/api/crawler/crawl-single` | Synchronous single-URL fetch and Redis cache benchmark | `?url=http://localhost:8080/mock-web/index.html` |
| `GET` | `/api/crawler/page/html` | Serves raw HTML source code of a crawled page | `?url=http://localhost:8080/mock-web/index.html` |
| `GET` | `/api/crawler/storage/info` | Returns disk archive size, file count, and storage path | None |
| `GET` | `/api/crawler/export` | Exports dataset as JSON or CSV file download | `?format=json` or `?format=csv` |

---

# 10. Frontend Architecture (Guided Stepper & Dark Console)

The frontend at [`src/main/resources/static/index.html`](file:///media/shared_ntfs/5th_Semester/SD_Lab/System-Design-Exercises/ex-8-web-crawler/src/main/resources/static/index.html) is built as a zero-dependency, high-performance developer console:

1. **Step-by-Step Guided Demonstration Mode:**
   - 6 sequential stages with top progress stepper:
     `[1] Seeds` &rarr; `[2] Frontier` &rarr; `[3] Content` &rarr; `[4] PageRank` &rarr; `[5] Search` &rarr; `[6] Export`.
   - Dedicated `Previous` and `Next` navigation controls to pace the oral explanation.
   - Pacing control selector (`250ms`, `400ms`, `30ms`) allowing real-time observation of queues.
2. **Complete All-in-One Console Mode:** Toggle button allows viewing all modules simultaneously.
3. **Developer Dark Theme:** Palette tuned to `#090d16` canvas, `#101726` elevated cards, `#1e293d` borders, and zero childish emojis.
4. **Interactive HTML5 Canvas:** Visualizes graph vertices and directed edges, dynamically resizing node radii based on PageRank scores.
5. **Document Content Inspector Modal:** Direct toggle between readable clean text, structured headings, and raw HTML source code.

---

# 11. Viva Presentation Guide: Step-by-Step Script for Mam

Open **`http://localhost:8080`** in your browser and deliver the presentation following these 6 steps:

### Step 1: Seeds & Parameter Configuration
- **Script:** *"Good morning Mam. We begin in Step 1 by selecting a starting Seed URL. Our crawler uses Breadth-First Search (BFS) bounded by Max Depth = 3 and 4 concurrent worker threads. We set a politeness delay of 250 ms so we can visibly observe queue execution in Step 2."*
- **Action:** Click preset **`Root Portal`**, verify pacing is set to `250 ms`, and click **`Start Crawl & Proceed to Step 2 &rarr;`**.

### Step 2: Frontier Queue & Live Crawl Execution
- **Script:** *"Mam, here in Step 2 you can see the multi-threaded workers pulling tasks from our Redis FIFO queue. Notice how our visited set automatically avoids 30+ cyclic loops, while RobotsTxtService blocks `/private-admin.html`."*
- **Action:** Point to the live progress bar advancing from 0 to 20 pages, then click **`Next: Inspect Extracted Contents &rarr;`**.

### Step 3: Content Extraction & Physical Archival
- **Script:** *"Mam, here is our dataset of crawled pages. To satisfy the requirement of taking down all content, we extract both clean text for search and raw HTML for archival, storing them on physical disk under `crawled_data/pages/`. Notice that row 15 is flagged as `CLONE` because its SHA-256 hash matches article 1."*
- **Action:** Click **`Inspect`** on any row, toggle between **`Clean Text`** and **`Raw HTML`**, click **`Open Source`**, then click **`Next: Link Graph & PageRank &rarr;`**.

### Step 4: Discovered Link Topology & Google PageRank
- **Script:** *"Mam, here we model the discovered hyperlinks as a directed graph $G=(V,E)$. We execute Google's PageRank power iteration with damping factor $d=0.85$. The node circle sizes scale dynamically with authority score, placing the Home Portal at rank 1 with score 100.0."*
- **Action:** Point out the canvas graph and the leaderboard, then click **`Next: BM25 Full-Text Search &rarr;`**.

### Step 5: Full-Text Search & Relevance Ranking
- **Script:** *"Mam, all body text was indexed into an Inverted Index. When we type a query, our Trie data structure provides instant autocomplete suggestions. Matches are scored using Okapi BM25 combined with PageRank."*
- **Action:** Type `quantum`, click the suggestion, press Enter, point out the highlighted snippets and score pills, then click **`Next: Discovered Registry & Export &rarr;`**.

### Step 6: Complete Discovered URLs Registry & Export
- **Script:** *"Mam, this final registry fulfills the requirement to take down every discovered website. It logs all 24 discovered URLs with their parent page referrer provenance. We can export the entire archive as JSON or CSV."*
- **Action:** Point out the Referrer column and the **Download Full JSON** export button.

---

# 12. Comprehensive Viva Examination Q&A (15 Core Questions)

### Q1: Why did you choose Breadth-First Search (BFS) instead of Depth-First Search (DFS)?
**Answer:** The web is a cyclic graph containing spider traps (infinite dynamic calendars, loops). DFS dives infinitely deep down a single path, overloading a single server and missing other seed websites. BFS explores layer-by-layer, prioritizing authoritative top-level pages, strictly honoring `maxDepth`, and distributing load fairly across hosts.

### Q2: Why is a Visited-URL Set necessary? What data structure is used?
**Answer:** Without a visited set, hyperlinked pages with circular references ($A \to B \to A$) cause the crawler to enter an infinite loop. We use a **Redis Set** (`crawler:visited:set`) leveraging the atomic `SADD` operation, providing $O(1)$ lookup and insertion while eliminating race conditions between concurrent worker threads.

### Q3: How does your crawler prevent race conditions when multiple threads access the queue?
**Answer:** In Redis, `RPUSH` and `LPOP` are atomic single-threaded operations. In our Java in-memory fallback, we use `ConcurrentLinkedQueue` (a lock-free CAS-based FIFO queue) and `ConcurrentHashMap.newKeySet()`, ensuring thread safety without locking bottlenecks.

### Q4: What is the difference between URL Deduplication and Content Deduplication?
**Answer:** URL deduplication checks if the *string address* was already seen. Content deduplication calculates a **SHA-256 cryptographic hash** over the clean body text. This detects when two different URLs serve the exact same article (mirrors or syndicated articles), preventing duplicate entries in our search index.

### Q5: How do you clean raw HTML into searchable text?
**Answer:** Using Jsoup, we parse the DOM, remove non-content tags (`<script>`, `<style>`, `<noscript>`, `<svg>`, `<iframe>`), extract heading outlines ($H1-H3$), and invoke `doc.body().text()`. This removes all tags, decodes HTML entities, and normalizes whitespace into natural sentences.

### Q6: Where and how is Redis used in your project?
**Answer:** Redis serves three distinct roles:
1. **Frontier Queue:** Redis List (`RPUSH` to enqueue, `LPOP` to dequeue in FIFO order).
2. **Visited Set:** Redis Set (`SADD` / `SISMEMBER` for atomic $O(1)$ loop prevention).
3. **Document Cache:** Redis Hash (`SETEX` / `GET`) providing sub-millisecond retrieval (0.4 ms) for pre-parsed page metadata.

### Q7: What is an Inverted Index and why is it better than a Forward Index?
**Answer:** A Forward Index maps Documents &rarr; Words, requiring a slow linear scan of all documents during search ($O(N \times L)$). An Inverted Index maps Words &rarr; List of Documents. Query lookup takes $O(1)$ hash table time to immediately find the postings list.

### Q8: What is Okapi BM25 and why is it superior to TF-IDF?
**Answer:** BM25 improves on TF-IDF using **Term Frequency Saturation** (diminishing returns for repeated words so spamming a word doesn't grant infinite score) and **Document Length Normalization** (penalizing long, verbose documents and boosting concise matches).

### Q9: How does the Google PageRank algorithm work?
**Answer:** PageRank models the web as a random surfer who clicks links with damping probability $d = 0.85$ and jumps randomly with probability $0.15$. A page gains authority if many pages link to it, especially if those referring pages have high authority themselves. We calculate this using power iteration until convergence ($\epsilon < 10^{-4}$).

### Q10: How do you combine BM25 and PageRank in your search engine?
**Answer:** We compute a composite score:
$$\text{Score} = 0.6 \times \text{BM25 Relevance} + 0.4 \times \text{PageRank Authority}$$
This ensures search results are both topically relevant to the query and structurally reputable.

### Q11: How does the Trie Autocomplete work?
**Answer:** All unique vocabulary words from crawled documents are inserted into a prefix tree (Trie). When the user types a prefix, the Trie traverses down to that prefix node in $O(L)$ time (where $L$ is prefix length) and returns matching words sorted by document frequency.

### Q12: How do you respect website politeness and robots.txt?
**Answer:** We parse `/robots.txt` before crawling a domain using `RobotsTxtService`. If an admin specifies `Disallow: /mock-web/private-admin.html`, our crawler checks the path prefix and skips it. We also include a configurable politeness delay (`politenessDelayMs = 250ms`) between requests.

### Q13: What files are saved on disk to "take down" all website content?
**Answer:** In `crawled_data/pages/`, every page gets three files: `{hash}.html` (raw markup), `{hash}.txt` (clean text), and `{hash}.json` (metadata). In `crawled_data/`, we also maintain `discovered_websites.json`, `discovered_urls.txt`, `all_crawled_content.json`, and `crawled_summary.csv`.

### Q14: How does the crawler determine when a crawl job is finished?
**Answer:** In `WebCrawlerEngine.java`, a crawl terminates when either:
1. The frontier queue is empty AND all worker threads are idle (`activeWorkers == 0`).
2. The maximum page limit (`maxPages`) is reached.
3. The user manually clicks "Stop Engine".

### Q15: How did you evaluate system performance?
**Answer:** We conducted concurrency benchmarks using Apache JMeter and Python load test scripts (`scripts/load_test.py concurrent`). Under multi-threaded workloads with 4 workers, our crawler achieved throughputs of **35+ pages per second**, while Redis cache hits responded in **0.4 ms** compared to 15 ms for live network fetches.

---
*End of Documentation — UCS3513 System Design Laboratory Exercise 8*
