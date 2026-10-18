#!/usr/bin/env python3
"""Generate the Vidya LMS Mini Project report (.docx) in the SSN System Design
Lab template style (SD_A4/A5/A6/A8 format).

Output: report/Vidya_LMS_MiniProject_Report.docx

Structure (matches ex-8 and below):
  Cover -> Aim -> Question -> Procedure -> Requirement Analysis & Component
  Mapping -> [page break] System Architecture & Workflow -> Design of Vidya
  LMS -> Output Results -> Analysis -> API / Command Reference & Test Results
  -> Learning Outcomes.

At every location where the team wants a ChatGPT-generated diagram, a
numbered "image slot" is reserved with the full image-generation prompt in a
code box plus the final figure caption, so a pasted image drops straight into
the numbered sequence.
"""

import os
import sys

from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Pt, Twips

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # mini-project/
sys.path.insert(0, os.path.join(os.path.dirname(ROOT), "shared"))
import docx_template as T  # noqa: E402

SHOTS = os.path.join(ROOT, "results", "screenshots")
FIGS = os.path.join(ROOT, "results", "figures")

# Diagram edition: set the env var VIDYA_DIAGRAM_MODE=1 to replace every ChatGPT
# prompt slot with an actual rendered diagram (report/diagrams/d01..d09) and write
# a SEPARATE docx. The prompt edition stays untouched.
DIAGRAM_MODE = os.environ.get("VIDYA_DIAGRAM_MODE") == "1"
DIAGRAM_DIR = os.path.join(ROOT, "report", "diagrams")
OUT_DOCX = os.path.join(
    ROOT, "report",
    "Vidya_LMS_MiniProject_Report_Diagrams.docx" if DIAGRAM_MODE
    else "Vidya_LMS_MiniProject_Report.docx")

TEAM = [
    ("Tushyent N P", "3122 24 5001 189"),
    ("Venkat Prabhu S", "3122 24 5001 196"),
    ("Vignesh Raaj S", "3122 24 5001 198"),
    ("Vino S R Harrison", "3122 24 5001 199"),
]

FIG = {"n": 0}


# --------------------------------------------------------------------------- #
# figure helpers
# --------------------------------------------------------------------------- #
def add_caption(doc, text):
    """Centred 10 pt bold caption line."""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Twips(160)
    r = p.add_run(text)
    r.font.size = Pt(10)
    r.bold = True
    return p


def add_real_figure(doc, path, caption_text, width=6.0):
    FIG["n"] += 1
    T.add_figure(doc, path, caption=f"Figure {FIG['n']}: {caption_text}", width=width)


def add_prompt_slot(doc, caption_text, prompt_text, diagram_file=None):
    """Reserve a numbered figure slot for a user-generated image.

    Prompt edition (default): a labelled sub-heading, a bordered code box with
    the exact ChatGPT image prompt, and the ready-made figure caption. Paste the
    generated image between the prompt box and the caption.

    Diagram edition (VIDYA_DIAGRAM_MODE=1): if the pre-rendered diagram exists it
    is embedded directly under this slot's number, replacing the prompt box.
    """
    FIG["n"] += 1
    n = FIG["n"]
    if DIAGRAM_MODE and diagram_file:
        path = os.path.join(DIAGRAM_DIR, diagram_file)
        if os.path.exists(path):
            T.add_figure(doc, path, caption="Figure %d: %s" % (n, caption_text), width=6.0)
            return
    T.add_subheading(doc, "Figure %d - Image slot: generate with the prompt below, "
                          "then paste the image here" % n)
    T.add_code(doc, "ChatGPT image prompt:\n" + prompt_text)
    add_caption(doc, "Figure %d: %s" % (n, caption_text))


# --------------------------------------------------------------------------- #
# image-generation prompts (shown at each diagram slot in the report)
# --------------------------------------------------------------------------- #
PR_ARCH = (
    "Generate a clean flat-vector system architecture diagram for 'Vidya LMS', a scalable\n"
    "university Library Management System. White background, professional navy (#0C2340)\n"
    "and blue (#1460A3) with emerald accents, Arial-style labels, thin labelled arrows.\n"
    "Layers top to bottom:\n"
    "1) Client tier: React SPA browser, OPAC kiosk, mobile barcode scanner.\n"
    "2) Edge: Nginx reverse proxy + Redis-Lua Token Bucket Rate Limiter guarding\n"
    "   /api/auth (10 tokens, 2/s) and /api/books/search (1500 tokens, 750/s);\n"
    "   label 'HTTP 429 + Retry-After on overflow'.\n"
    "3) Service tier 'Spring Boot 3.3 (:8080)': Auth & Members, Catalogue + Prefix-Trie\n"
    "   Autocomplete, Circulation & Reservations, Idempotent Payments, Async Web Crawler,\n"
    "   Consistent-Hash router.\n"
    "4) Data tier: Redis cache-aside KV store; MongoDB :27017 with 'Shard 1 - Members\n"
    "   (hashed memberId)' and 'Shard 2 - Books (category / ISBN)'; Loan store;\n"
    "   Snowflake 64-bit ID generator (Worker 1, DC 1); Base62 URL Shortener.\n"
    "Annotate 4 key flows: borrow = atomic findAndModify; reads = Redis cache-aside;\n"
    "crawler = publisher feed with ISBN dedup; short link = GET /s/{code} 302 redirect.\n"
    "Add a small legend. No photorealism - crisp boxes, subtle shadows."
)

PR_DATA = (
    "Generate a document-model / ER diagram for the MongoDB collections of a library\n"
    "system, crow's-foot notation, clean flat style, white background.\n"
    "Entities and key fields:\n"
    "members (_id Snowflake, memberId, email unique, passwordHash, role, maxBorrowLimit)\n"
    "books (_id, isbn unique indexed, title, authors[], categories[], totalCopies,\n"
    "availableCopies, externalRating, @Version)\n"
    "book_copies (_id, bookId -> books, barcode unique, status enum AVAILABLE /\n"
    "BORROWED / RESERVED_FOR_PICKUP / MAINTENANCE, currentHolderId, currentLoanId)\n"
    "loans (loanId LOAN-xxxx, memberId, copyId -> book_copies, bookId, dueDate,\n"
    "renewalCount, status ACTIVE / RETURNED / OVERDUE)\n"
    "fines (fineId, memberId, loanId, overdueDays, amount at Rs.1.50/day, cap Rs.50)\n"
    "payments (paymentId PAY-xxxx, fineId, memberId, amount, idempotencyKey unique,\n"
    "transactionRef SIM-TX-xxxx, status)\n"
    "reservations (reservationId RES-xxxx, bookId, memberId, queuePosition, status)\n"
    "short_urls (code Base62 -> targetUrl, resourceType, clickCount)\n"
    "Annotate shard keys (members: hashed memberId; books: category/_id; loans:\n"
    "compound memberId+loanId) and the Redis keys cache:book:{id} (10 min TTL) and\n"
    "search:books:{hash} (60 s TTL)."
)

PR_SEQ = (
    "Generate a clean UML sequence diagram titled 'Atomic Borrow - Zero Double-Borrowing'.\n"
    "Lifelines: Member A (Thread 1), Member B (Thread 2), Circulation API, MongoDB,\n"
    "Redis. Messages in order:\n"
    "1) Member A and Member B both send POST /api/circulation/borrow for the same title.\n"
    "2) In parallel the API issues atomic findAndModify(copy: status AVAILABLE ->\n"
    "   BORROWED) for both threads.\n"
    "3) MongoDB returns the claimed copy to Thread 1 and null to Thread 2.\n"
    "4) API invalidates Redis key cache:book:{id} (write-through eviction).\n"
    "5) Member A receives 200 OK with created loan and due date; Member B receives\n"
    "   409 Conflict 'All copies checked out - reservation available'.\n"
    "Add a note box: 'Guarantee: exactly one loan per physical copy - verified with\n"
    "20-thread race test (1 success, 19 conflicts)'. Classy UML look, no emoji."
)

PR_BUCKET = (
    "Generate a textbook-style diagram of a Token Bucket rate limiter.\n"
    "Show a bucket of capacity C=10 tokens refilling at r=2 tokens per second.\n"
    "Incoming request arrows on the left labelled 'HTTP 200 OK - token consumed'\n"
    "while tokens remain, and 'HTTP 429 Too Many Requests + Retry-After' when empty.\n"
    "Write the formula tokens(t) = min(C, tokens(t-1) + elapsed * r).\n"
    "Right panel: 'Redis atomic Lua EVAL with 60 s TTL' and 'fallback: in-memory\n"
    "ConcurrentHashMap bucket' with a small failover arrow.\n"
    "Annotate the measured burst: 50 concurrent logins -> 10 permitted, 40 throttled\n"
    "(80% rejection). Blue/red palette, clean educational infographic."
)

PR_RING = (
    "Generate a consistent-hashing ring infographic. Draw a large circle for the 2^32\n"
    "keyspace containing servers Node-A, Node-B, Node-C (blue) plus a newly added\n"
    "Node-D (emerald), each with many small virtual-node ticks (160 vnodes, MD5).\n"
    "Keys K1..K6 as dots that map clockwise to the next virtual node, with arrows.\n"
    "Two inset panels:\n"
    "  '3 -> 4 nodes, consistent hashing: only 28.3% of 2000 keys remapped'.\n"
    "  'modulo hash(key) % N: 75.1% of keys remapped'.\n"
    "Caption note: 'only ~1/N keys migrate - no cache stampede'. Clean modern\n"
    "infographic, spiral tick marks, legend box."
)

PR_URL = (
    "Generate a flow diagram for a Base62 URL shortener. Step 1: POST /api/urls/shorten\n"
    "receives a long catalogue URL. Step 2: a 64-bit Snowflake ID is generated.\n"
    "Step 3: bijective Base62 encoding [0-9a-zA-Z] produces a short code e.g. '23Vp6b'.\n"
    "Step 4: mapping stored in MongoDB/Redis with click counter.\n"
    "Bottom flow: GET /s/{code} -> O(1) indexed lookup -> HTTP 302 Found with\n"
    "Location: original URL, incrementing analytics. Add a small kiosk QR-scan usage\n"
    "icon. Annotate measured latency: mean 5.10 ms, P95 6.83 ms. Flat clean style."
)

PR_CRAWLER = (
    "Generate a pipeline flowchart for an asynchronous crawler ingestion job.\n"
    "Left: external mock publisher REST feed /mock-external-library/books returning\n"
    "7 JSON book records. Middle: 'AsyncExecutor worker pool (thread task-1)' box that\n"
    "fetches, parses and normalizes records. Then a decision diamond 'ISBN already in\n"
    "catalogue?'. Yes -> 'Enrich record (rating, summary) - DEDUPLICATED'. No ->\n"
    "'Import title + physical copies - IMPORTED'. Right: MongoDB catalogue plus a\n"
    "CrawlerJob document logging fetched / parsed / imported / deduplicated / failed\n"
    "counters and duration. Annotate the measured run: 7 fetched, 7 deduplicated,\n"
    "0 imported, 77 ms, 90.9 books/s. Clean flowchart, arrows left to right."
)

PR_TRIE = (
    "Generate a prefix-trie (autocomplete) diagram. Root node at the top; path\n"
    "r-o-o-t -> d -> i -> s -> t highlighted for the query 'dist'. Under the 't'\n"
    "node list top-5 suggestions with small score tags:\n"
    "  'Distributed Systems' (CATEGORY), 'Distributed Systems: Principles and\n"
    "   Paradigms' (TITLE), 'Designing Data-Intensive Applications' (TITLE).\n"
    "Right-hand annotation panel: 'lookup O(L + K log K) - independent of catalogue\n"
    "size', 'in-memory, rebuilt at startup', 'measured: P@5 = 0.46, R@5 = 0.80,\n"
    "avg 2.32 ms'. Show a browser search bar mock with the dropdown open under the\n"
    "query 'dist'. Light infographic style, monospace node labels."
)

PR_SHARD = (
    "Generate a MongoDB sharding diagram. A router (mongos) on the left receives\n"
    "member and book writes. Show three shards: 'Shard A - Members (hashed memberId)',\n"
    "'Shard B - Books (category range / hashed _id)', 'Shard C - Loans (compound\n"
    "memberId + loanId)'. Draw servers as stacked cylinders; annotate uniform ~33%\n"
    "distribution and 'hashed sharding avoids write hotspots on sequential inserts'.\n"
    "Add a small callout: 'category-filtered catalogue query routed to a single\n"
    "shard - avoids scatter-gather'. Clean datacenter infographic style."
)


# --------------------------------------------------------------------------- #
# report builder
# --------------------------------------------------------------------------- #
def build_report():
    doc = T.new_document()

    # ---------------- Cover ------------------------------------------------ #
    T.add_cover(
        doc,
        college="Sri Sivasubramaniya Nadar College of Engineering",
        autonomous="(An Autonomous Institution, Affiliated to Anna University, Chennai)",
        dept="DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING",
        year_line="III Year CSE (V Semester)  |  Academic Year 2026-27",
        course_code="UCS3513 - SYSTEM DESIGN LABORATORY",
        lab_exercise="Mini Project 3",
        title="Vidya LMS - Scalable Library Management System",
        team=TEAM,
    )

    # ---------------- 1. Aim ----------------------------------------------- #
    T.add_section(doc, "Aim", before=40)
    T.add_bullet(doc, "To design and develop Vidya LMS, a scalable Library Management System prototype "
                      "covering member registration and authentication, catalogue search and browsing, book "
                      "availability, borrowing, returning, reservations, and fine/payment management.")
    T.add_bullet(doc, "To analyse the requirements and map the eight prescribed system-design components "
                      "(Rate Limiter, Sharding, Consistent Hashing, Key-Value Store, Unique ID Generator, "
                      "URL Shortener, Web Crawler, Autocomplete) to the appropriate library functionalities "
                      "with justifications, and to implement them end-to-end using Spring Boot 3.3 (Java 17), "
                      "MongoDB, Redis and a React 18 / TypeScript frontend.")
    T.add_bullet(doc, "To evaluate the system quantitatively under three workload tiers (Low / Medium / High), "
                      "compare optimised search against a plain baseline, and report the component-specific "
                      "metrics for every system-design component.")
    T.add_bullet(doc, "To analyse scalability and failure scenarios (cluster expansion, scarce-inventory "
                      "contention, Redis outage) and discuss the bottlenecks, trade-offs and limitations.")

    # ---------------- 2. Question ------------------------------------------ #
    T.add_section(doc, "Question")
    T.add_body(doc, "Design and develop a prototype of a scalable Library Management System supporting member "
                    "registration and authentication, book search and browsing, book availability, borrowing and "
                    "returning, reservation management, and fine/payment management. The system must handle "
                    "increasing numbers of members, books, transactions, and concurrent requests while providing "
                    "efficient and reliable access to library services.")
    T.add_body(doc, "The Rate Limiter, Sharding, Consistent Hashing, Key-Value Store, Unique ID Generator, URL "
                    "Shortener, Web Crawler, and Autocomplete components are provided, but the mapping between "
                    "components and library functionalities is not given. The analysis must determine which "
                    "component(s) suit each functionality and justify every choice; a component may serve several "
                    "functionalities and a functionality may need several components.")
    T.add_body(doc, "Book and member data must be capable of being partitioned across multiple database shards, "
                    "consistent hashing may be used to distribute data or requests across servers, and the web "
                    "crawler must work against simulated external book sources for details, ratings or availability. "
                    "The project must include quantitative performance evaluation - average response time, "
                    "throughput, success/error rate and resource usage under at least three workload levels, plus "
                    "component-specific metrics (rejection rate, shard distribution, hashing load balance, KV "
                    "lookup latency, ID generation rate and collisions, redirection latency, crawling throughput, "
                    "and autocomplete response time and accuracy) - a comparison against a simple baseline where "
                    "appropriate, and at least one scalability or failure scenario with a discussion of "
                    "bottlenecks, scalability, reliability and trade-offs.")

    # ---------------- 3. Procedure ----------------------------------------- #
    T.add_section(doc, "Procedure")
    T.add_bullet(doc, "Analyse the requirements and map the eight system-design components to specific Vidya LMS "
                      "functionalities; record the justification for each mapping (Table 1).")
    T.add_bullet(doc, "Model the domain (members, books, physical copies, loans, reservations, fines, payments, "
                      "short links) as MongoDB documents with unique indexes on email, ISBN, barcode and "
                      "idempotency keys.")
    T.add_bullet(doc, "Implement the token-bucket rate limiter in Redis using an atomic Lua script with a "
                      "thread-safe in-memory fallback; apply strict limits to authentication (capacity 10, "
                      "refill 2/s) and generous limits to public reads (capacity 1500, refill 750/s).")
    T.add_bullet(doc, "Configure database sharding keys: hashed memberId for members, hashed _id / category for "
                      "books, and a compound memberId + loanId key for loans.")
    T.add_bullet(doc, "Implement an MD5 consistent-hash ring with 160 virtual nodes per server in a TreeMap, and "
                      "expose a 3-to-4 node remap simulation benchmark.")
    T.add_bullet(doc, "Implement Redis cache-aside reads for catalogue detail (10-minute TTL) and search results "
                      "(60-second TTL) with write-through cache eviction on every inventory change.")
    T.add_bullet(doc, "Implement the 64-bit Snowflake ID generator (41-bit timestamp, 10-bit datacenter + "
                      "worker, 12-bit sequence) as the source of barcodes, loans, reservations, fines, payments "
                      "and short URLs, and the Base62 URL shortener for shareable catalogue links with a 302 "
                      "redirect and click analytics.")
    T.add_bullet(doc, "Implement the asynchronous web crawler with a dedicated task executor, external publisher "
                      "feed ingestion, ISBN de-duplication and enrichment.")
    T.add_bullet(doc, "Implement the in-memory prefix-trie autocomplete indexing titles, authors and categories "
                      "with ranked suggestions and a precision@5 evaluation endpoint.")
    T.add_bullet(doc, "Implement atomic circulation with MongoDB findAndModify, renewal cap and reservation "
                      "wait-list interlock, and idempotent payments keyed by client UUID.")
    T.add_bullet(doc, "Build the React UI (catalogue, search, autocomplete, loans, fines, admin desk, system "
                      "design lab), expose the REST APIs through Swagger / OpenAPI, then execute the JUnit "
                      "suite, the 14-step workflow verification and the benchmark harness to tabulate results.")

    # ---------------- 4. Requirement Analysis & Component Mapping ---------- #
    T.add_section(doc, "Requirement Analysis & Component Mapping")
    T.add_body(doc, "Each prescribed component was matched to the library functionality where it removes a real "
                    "scalability or reliability bottleneck. Table 1 lists the mapping and the architectural "
                    "justification; the implementation of every row is demonstrated in the Design and Output "
                    "Results sections.")
    T.add_table_label(doc, "Table 1: System-Design Component to Functionality Mapping with Justifications")
    T.add_table(
        doc,
        ["Component", "Library Functionality", "Architectural Justification"],
        [
            ["1. Rate Limiter", "Authentication (/api/auth/**) and Catalogue Search (/api/books/search)",
             "Token bucket stops brute-force credential stuffing and protects the search index from resource "
             "exhaustion; allows legitimate bursts while capping sustained rate. Auth and public-read traffic use "
             "separate buckets."],
            ["2. Sharding", "Member storage and book catalogue storage",
             "Removes single-node storage and IOPS limits: members are partitioned by hashed memberId, books by "
             "hashed _id / category, loans by compound memberId + loanId for query locality."],
            ["3. Consistent Hashing", "Request / cache-server routing across library nodes",
             "MD5 virtual-node ring spreads load uniformly; adding or losing a node migrates only ~1/N keys, "
             "preventing cache stampedes - measured 28.3% vs 75.1% for modulo hashing."],
            ["4. Key-Value Store", "Catalogue detail and search-result reads (cache-aside)",
             "Redis cache-aside absorbs repetitive catalogue browsing; write-through eviction keeps availability "
             "counts truthful. Search-cache hit ratio reached 98.9% during load runs."],
            ["5. Unique ID Generator", "Barcodes, loans, reservations, fines, payments, short links",
             "64-bit Snowflake IDs are time-ordered, coordination-free and generated locally: 50,000 IDs over 20 "
             "threads with zero collisions, no database sequence locks."],
            ["6. URL Shortener", "Shareable catalogue links (/s/{code})",
             "Bijective Base62 over Snowflake IDs yields compact links for QR kiosks and citations with O(1) 302 "
             "redirects and click analytics - mean 5.10 ms."],
            ["7. Web Crawler", "Metadata ingestion and catalog enrichment",
             "Asynchronous worker pool ingests simulated external publisher feeds, de-duplicates by ISBN and "
             "enriches records; measured throughput 90.9 books/s with 100% dedup precision."],
            ["8. Autocomplete", "Instant search-bar suggestions (/api/autocomplete)",
             "In-memory ranked prefix trie returns top-5 suggestions in ~2.3 ms independent of catalogue size; "
             "evaluated Precision@5 = 0.46, Recall@5 = 0.80."],
        ],
    )

    # ---------------- 5. System Architecture & Workflow (page break) ------- #
    T.add_page_break(doc)
    T.add_section(doc, "System Architecture & Workflow", before=40)
    T.add_body(doc, "Vidya LMS is a four-tier system. The client tier is a React 18 SPA with an OPAC-style "
                    "catalogue. The edge tier applies the Redis-Lua token-bucket rate limiter before requests "
                    "reach the Spring Boot service tier, which hosts authentication, catalogue + autocomplete, "
                    "circulation, payments, the asynchronous crawler and the consistent-hash router. The data tier "
                    "combines a Redis key-value cache, a sharded MongoDB deployment for members, books and loans, "
                    "the Snowflake ID generator and the Base62 short-link store.")
    add_prompt_slot(doc, "High-level architecture of Vidya LMS - clients, rate-limited edge, Spring Boot "
                         "services, and the distributed data and cache tier.", PR_ARCH,
                    diagram_file="d01_architecture.png")
    add_prompt_slot(doc, "Document model of the Vidya LMS collections with shard keys and Redis cache keys.",
                    PR_DATA, diagram_file="d02_data_model.png")
    add_prompt_slot(doc, "Atomic borrow sequence: two concurrent members racing for the last physical copy, "
                         "resolved by a single atomic findAndModify.", PR_SEQ,
                    diagram_file="d03_borrow_sequence.png")
    if DIAGRAM_MODE:
        T.add_body(doc, "The working prototype itself is demonstrated by the verification screenshots in the "
                        "Output Results section.")
    else:
        T.add_body(doc, "All diagrams above are generated from the specified prompts; the working prototype "
                        "itself is demonstrated by the verification screenshots in the Output Results section.")

    # ---------------- 6. Design of Vidya LMS ------------------------------- #
    T.add_section(doc, "Design of Vidya LMS")

    T.add_subheading(doc, "1. Token Bucket Rate Limiter (Redis Lua + in-memory fallback)")
    T.add_body(doc, "Every rate-limited request atomically consumes a token through a Redis Lua script. When the "
                    "bucket is empty the caller receives HTTP 429 with a Retry-After header; when Redis is "
                    "unreachable the limiter degrades to a thread-safe in-memory bucket so requests are never "
                    "dropped. Auth endpoints are strict (capacity 10, refill 2/s) while public reads are generous "
                    "(capacity 1500, refill 750/s) so that browsing and benchmarks are not throttled. All limits "
                    "are externalised as ratelimit.* properties:")
    T.add_code(doc,
               "ratelimit.auth.capacity=10          # strict: login / registration\n"
               "ratelimit.auth.refill-per-second=2\n"
               "ratelimit.search.capacity=1500      # generous: public catalogue reads\n"
               "ratelimit.search.refill-per-second=750\n"
               "ratelimit.default.capacity=40\n"
               "ratelimit.default.refill-per-second=10")
    add_prompt_slot(doc, "Token bucket mechanics with the strict auth policy and the measured 80% burst "
                         "rejection.", PR_BUCKET, diagram_file="d04_token_bucket.png")

    T.add_subheading(doc, "2. Database Sharding")
    T.add_body(doc, "Sharding keys are declared per collection so member, book and loan data can be partitioned "
                    "horizontally without hotspots. Members use a hashed memberId key, books a hashed _id / "
                    "category key, and loans a compound memberId + loanId key that keeps a member's loan history "
                    "on one shard. The live shard-key configuration is exposed by GET /api/sharding/stats:")
    T.add_code(doc,
               "members : { \"memberId\": \"hashed\" }   -> uniform member partitions\n"
               "books   : { \"_id\": \"hashed\" }        -> uniform write/read spread\n"
               "loans   : { \"memberId\": 1, \"loanId\": 1 } -> member query locality")
    add_prompt_slot(doc, "Shard topology: hashed member and book partitions plus compound-key loan shards.",
                    PR_SHARD, diagram_file="d05_sharding.png")

    T.add_subheading(doc, "3. Consistent Hashing Router")
    T.add_body(doc, "A TreeMap-based ring hashes servers and keys with MD5 into the 2^32 keyspace. Each physical "
                    "server owns 160 virtual nodes to smooth the distribution. When the cluster grows from N to "
                    "N+1 nodes only about 1/(N+1) of keys move, versus roughly N/(N+1) under modulo hashing:")
    T.add_code(doc,
               "// ConsistentHashRing.java (core idea)\n"
               "TreeMap<Long, String> circle = new TreeMap<>();\n"
               "for (int v = 0; v < 160; v++)                 // virtual nodes\n"
               "    circle.put(md5Hash(node + \"#\" + v), node);\n"
               "String target = circle.ceilingEntry(md5Hash(key)) != null\n"
               "        ? circle.ceilingEntry(md5Hash(key)).getValue()\n"
               "        : circle.firstEntry().getValue();     // wrap around")
    add_prompt_slot(doc, "Consistent-hash ring with virtual nodes and the 3-to-4 node remap comparison.",
                    PR_RING, diagram_file="d06_hash_ring.png")

    T.add_subheading(doc, "4. Key-Value Store (Redis Cache-Aside)")
    T.add_body(doc, "Catalogue detail reads use cache:book:{id} with a 10-minute TTL; search results use a "
                    "60-second TTL keyed by the normalised query. Every inventory mutation (borrow, return, copy "
                    "allocation, crawler import) evicts the affected keys so availability counts are never stale. "
                    "During the load runs the search cache served 1,829 hits against 21 misses (98.9% hit ratio).")
    T.add_code(doc,
               "// BookService.java - read path (cache-aside)\n"
               "String json = redis.opsForValue().get(\"cache:book:\" + id);\n"
               "if (json != null) { hits++; return objectMapper.readValue(json, Book.class); }\n"
               "Book book = bookRepository.findById(id).orElseThrow();   // miss -> MongoDB\n"
               "redis.opsForValue().set(key, json(book), Duration.ofMinutes(10));\n"
               "\n"
               "// write path (borrow/return/copy allocation)\n"
               "cacheEvictionService.evictBook(bookId); // immediate invalidation")

    T.add_subheading(doc, "5. Snowflake 64-bit Unique ID Generator")
    T.add_body(doc, "All identifiers (barcodes, loans LOAN-xxxx, reservations RES-xxxx, fines, payments PAY-xxxx "
                    "and short codes) come from a local Snowflake generator: 41-bit millisecond timestamp, 10-bit "
                    "datacenter + worker, 12-bit sequence, giving 4,096 IDs per millisecond per worker with zero "
                    "coordination. The JUnit stress test generated 50,000 IDs across 20 parallel threads with zero "
                    "collisions.")
    T.add_table_label(doc, "Table 2: Snowflake 64-bit Layout (as shown in the live System Design Lab)")
    T.add_table(
        doc,
        ["Bits", "Field", "Range / Meaning"],
        [
            ["1", "Sign", "Unused, always 0"],
            ["41", "Timestamp", "Milliseconds since custom epoch (approx 69 years)"],
            ["5", "Datacenter ID", "0 - 31 (this deployment: DC 1)"],
            ["5", "Worker ID", "0 - 31 (this deployment: worker 1)"],
            ["12", "Sequence", "0 - 4095 per millisecond (4,096 / ms / worker)"],
        ],
    )
    add_real_figure(doc, os.path.join(SHOTS, "r16_systemdesign_snowflake.png"),
                    "Live System Design Lab - Snowflake bit-layout anatomy and a freshly generated 64-bit ID "
                    "(0 collisions across 50,000 stress-test IDs).")

    T.add_subheading(doc, "6. URL Shortener (Base62)")
    T.add_body(doc, "Shareable catalogue links are produced by bijectively encoding 64-bit Snowflake IDs in "
                    "Base62. GET /s/{code} performs an O(1) indexed lookup and answers with HTTP 302 plus the "
                    "Location header, incrementing the click counter. Measured redirection latency: mean 5.10 ms, "
                    "P95 6.83 ms.")
    T.add_code(doc,
               "// Base62Encoder.java\n"
               "private static final String ALPHABET =\n"
               "    \"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\";\n"
               "public static String encode(long value) {\n"
               "    if (value == 0) return \"0\";\n"
               "    StringBuilder sb = new StringBuilder();\n"
               "    while (value > 0) { sb.append(ALPHABET.charAt((int)(value % 62))); value /= 62; }\n"
               "    return sb.reverse().toString();\n"
               "}")
    add_prompt_slot(doc, "Base62 short-link generation and the GET /s/{code} redirect flow.", PR_URL,
                    diagram_file="d07_url_shortener.png")

    T.add_subheading(doc, "7. Asynchronous Web Crawler")
    T.add_body(doc, "The crawler job is submitted to a dedicated task executor (observed thread task-1) and "
                    "streams external publisher records into the catalogue. ISBN de-duplication distinguishes "
                    "imported titles from enriched duplicates and records every counter on the CrawlerJob "
                    "document. The latest measured run fetched and parsed 7 records in 77 ms - 90.9 books/s - with "
                    "100% de-duplication accuracy (7/7 already present).")
    T.add_code(doc,
               "// WebCrawlerService.java - asynchronous execution\n"
               "@Async(\"crawlerExecutor\")\n"
               "public void runJobAsync(String jobId) {\n"
               "    job.setStatus(RUNNING);\n"
               "    for (ExternalBook dto : fetchFeed(sourceUrl)) {\n"
               "        if (bookRepository.existsByIsbn(dto.getIsbn())) {\n"
               "            enrichExisting(dto); job.incrementDeduplicated();\n"
               "        } else {\n"
               "            importTitleWithCopies(dto); job.incrementImported();\n"
               "        }\n"
               "    }\n"
               "    job.complete();   // durationMs + throughput recorded\n"
               "}")
    add_prompt_slot(doc, "Crawler pipeline with the ISBN dedup decision and the measured ingestion run.",
                    PR_CRAWLER, diagram_file="d08_crawler_pipeline.png")

    T.add_subheading(doc, "8. Autocomplete (Ranked Prefix Trie)")
    T.add_body(doc, "Titles, authors and categories are indexed at startup into an in-memory multi-way trie. Each "
                    "node stores ranked top-k candidates so a lookup costs O(L + K log K) regardless of catalogue "
                    "size. The evaluation endpoint scores Precision@5 and Recall@5 against a labelled query set; "
                    "the live trie answered 5 probe queries in 2.32 ms on average and the UI panel reports 46.0% "
                    "Precision@5 with 80.0% Recall@5.")
    T.add_code(doc,
               "// PrefixTrie.java - insert + top-k retrieval\n"
               "public void insert(String text, String type) {\n"
               "    TrieNode node = root;\n"
               "    for (char c : text.toLowerCase().toCharArray()) {\n"
               "        node = node.children.computeIfAbsent(c, k -> new TrieNode());\n"
               "        node.addCandidate(text, type);   // ranked by type weight + popularity\n"
               "    }\n"
               "}\n"
               "public List<AutocompleteSuggestion> suggest(String prefix, int k) { /* O(L + K log K) */ }")
    add_prompt_slot(doc, "Ranked prefix trie structure and the live suggestion flow for the query 'dist'.",
                    PR_TRIE, diagram_file="d09_trie.png")

    T.add_subheading(doc, "9. Atomic Circulation, Renewals and Idempotent Payments")
    T.add_body(doc, "Availability is decided by the database engine itself: a single findAndModify transitions one "
                    "AVAILABLE copy to BORROWED and returns it only to exactly one caller. Renewals are capped at "
                    "two extensions, blocked by a reservation wait-list, and committed with a compare-and-set on "
                    "the current renewalCount. Payments echo the receipt for a repeated idempotency key instead of "
                    "charging twice.")
    T.add_code(doc,
               "// CirculationService.java - atomic claim (zero double borrowing)\n"
               "Query q = new Query(Criteria.where(\"_id\").is(copyId)\n"
               "        .and(\"status\").is(BookCopyStatus.AVAILABLE));\n"
               "Update u = new Update().set(\"status\", BORROWED)\n"
               "        .set(\"currentHolderId\", memberId).set(\"currentLoanId\", loanId);\n"
               "BookCopy claimed = mongoTemplate.findAndModify(q, u, options, BookCopy.class);\n"
               "if (claimed == null) throw new ConflictException(\"All copies checked out\");\n"
               "\n"
               "// PaymentService.java - idempotent replay\n"
               "Optional<Payment> existing = repo.findByIdempotencyKey(request.getIdempotencyKey());\n"
               "if (existing.isPresent()) return receipt(existing.get());   // identical receipt")

    # ---------------- 7. Output Results ------------------------------------ #
    T.add_section(doc, "Output Results")
    T.add_body(doc, "All measurements below were produced on the running system (Spring Boot 3.3 on Java 17, "
                    "MongoDB, Redis, React frontend) with a multi-threaded benchmark harness; raw JSON evidence "
                    "lives under results/raw/ and every table and figure is reproducible with "
                    "benchmarks/run_benchmarks.py.")

    T.add_subheading(doc, "1. Workload Tiers - Low / Medium / High Concurrency")
    add_real_figure(doc, os.path.join(FIGS, "scalability_workload_tiers.png"),
                    "Throughput and latency across the three workload tiers (Low 10, Medium 50, High 100 "
                    "concurrent users).")
    T.add_table_label(doc, "Table 3: Three Workload Tiers on the Catalogue Search / Availability Paths")
    T.add_table(
        doc,
        ["Metric", "Low (10 users)", "Medium (50 users)", "High (100 users)"],
        [
            ["Total requests executed", "200", "500", "1000"],
            ["Throughput (req/s)", "313.10", "466.93", "507.85"],
            ["Average latency (ms)", "30.08", "80.43", "113.29"],
            ["Median latency (ms)", "23.22", "78.30", "110.07"],
            ["P95 latency (ms)", "106.38", "129.47", "196.14"],
            ["P99 latency (ms)", "117.58", "152.32", "244.23"],
            ["HTTP status breakdown", "200 OK x200 (100%)", "200 OK x500 (100%)", "200 OK x1000 (100%)"],
            ["Error rate", "0.00%", "0.00%", "0.00%"],
        ],
    )
    T.add_body(doc, "Throughput rises from 313 to 508 req/s as concurrency grows while the error rate stays at "
                    "exactly 0.00% - the connection pools absorb the load without a single 5xx or timeout.")

    T.add_subheading(doc, "2. Baseline vs Optimised Search")
    T.add_body(doc, "The baseline executes an unindexed regex scan of the whole collection with in-memory "
                    "filtering and no cache. The optimised path uses a regex-escaped indexed query plus Redis "
                    "cache-aside. With only 35 titles (about 117 copies) the collection already fits in memory, so "
                    "the honest measured gain is modest: 38.27 ms -> 35.28 ms, a 1.08x latency improvement and "
                    "+8.68% throughput. The gap is expected to widen with catalogue size, where scan cost grows "
                    "linearly while the indexed path stays flat.")
    add_real_figure(doc, os.path.join(FIGS, "baseline_vs_optimized_search.png"),
                    "Baseline (unindexed regex scan) vs optimised (indexed + cached) search at 20 concurrent "
                    "users, 150 requests each.")
    T.add_table_label(doc, "Table 4: Search Architecture Comparison (20 Concurrent Users, 150 Requests)")
    T.add_table(
        doc,
        ["Architecture mode", "Avg latency (ms)", "Median (ms)", "P95 (ms)", "P99 (ms)", "Throughput (req/s)", "Status codes"],
        [
            ["Baseline (unindexed regex, no cache)", "38.27", "38.71", "49.52", "53.01", "447.24", "200 OK x150 (100%)"],
            ["Optimised (indexed + cached)", "35.28", "34.93", "49.44", "54.36", "486.05", "200 OK x150 (100%)"],
        ],
    )

    T.add_subheading(doc, "3. Component-Specific Measurements")
    T.add_table_label(doc, "Table 5: Component-Specific Metrics Summary")
    T.add_table(
        doc,
        ["Component", "Metric", "Measured value", "Observation"],
        [
            ["Rate Limiter", "Burst rejection rate", "80.0% (40/50 rejected)",
             "25 parallel bad logins drained the capacity-10 auth bucket: exactly 10 x HTTP 401 permitted, 40 x "
             "HTTP 429 throttled within 0.159 s; limiter overhead 2.58 ms average."],
            ["Consistent Hashing", "Key churn 3 -> 4 nodes", "28.3% remapped (566/2000)",
             "Modulo hashing remapped 75.1% (1502/2000). Theoretical expectations 25% / 75%; consistent hashing "
             "saves approx 62% of cache entries on expansion."],
            ["Autocomplete", "Precision@5 / Recall@5 / latency", "0.46 / 0.80 / 2.32 ms",
             "Five labelled probes (comp, data, algo, dist, clean); worst case 'clean' reached 1.00 both; the "
             "live UI reported 46.0% / 80.0% on the same metric."],
            ["Unique ID Generator", "Collisions over 50,000 IDs", "0 (100.0% unique)",
             "Generated across 20 parallel threads; live counter shows 100% unique with 0 collisions."],
            ["Key-Value Store", "Search-cache hit ratio", "98.9% (1829 hits / 21 misses)",
             "Under benchmark load; detail cache served at ~1 ms after first read with 10-minute TTL."],
            ["URL Shortener", "Redirection latency", "mean 5.10 ms, P95 6.83 ms",
             "O(1) lookup + HTTP 302; 50 cached resolutions averaged 1.12 ms in-app."],
            ["Web Crawler", "Ingestion throughput / dedup", "90.9 books/s; 7/7 deduplicated",
             "Latest run: 7 fetched, 7 parsed, 0 imported, 0 failed in 77 ms on an async executor thread."],
            ["Workload Tiers", "Peak sustained throughput", "507.85 req/s at 100 users",
             "0.00% error rate at every tier; P95 196 ms at the highest tier."],
        ],
    )
    add_real_figure(doc, os.path.join(FIGS, "consistent_hashing_churn.png"),
                    "Consistent-hash churn vs modulo hashing when the 3-node cluster gains a 4th node "
                    "(2000 keys, 160 virtual nodes).")
    add_real_figure(doc, os.path.join(FIGS, "ratelimiter_rejection_pie.png"),
                    "Rate-limiter burst outcome: 10 permitted vs 40 throttled (80% rejection) over 50 rapid "
                    "authentication attempts.")

    T.add_subheading(doc, "4. Autocomplete Evaluation Detail")
    T.add_table_label(doc, "Table 6: Per-Query Autocomplete Precision / Recall (k = 5)")
    T.add_table(
        doc,
        ["Prefix", "Relevant retrieved", "Suggestions returned", "Precision@5", "Recall@5"],
        [
            ["clean", "2", "2", "1.00", "1.00"],
            ["algo", "1", "2", "0.50", "1.00"],
            ["dist", "2", "5", "0.40", "1.00"],
            ["comp", "1", "5", "0.20", "0.50"],
            ["data", "1", "5", "0.20", "0.50"],
            ["Mean", "-", "-", "0.46", "0.80"],
        ],
    )

    T.add_subheading(doc, "5. Web Crawler Ingestion History")
    T.add_table_label(doc, "Table 7: Crawler Jobs Executed Against the Simulated Publisher Feed")
    T.add_table(
        doc,
        ["Job", "Fetched", "Parsed", "Imported", "Deduplicated", "Failed", "Duration", "Throughput", "Status"],
        [
            ["CRAWL-...0431", "7", "7", "0", "7", "0", "77 ms", "90.9 books/s", "COMPLETED"],
            ["CRAWL-...8928", "7", "7", "0", "7", "0", "88 ms", "79.5 books/s", "COMPLETED"],
            ["CRAWL-...4352", "7", "7", "2", "5", "0", "123 ms", "56.9 books/s", "COMPLETED"],
            ["CRAWL-...1200", "0", "0", "0", "0", "0", "154 ms", "0.0", "FAILED (403 - fixed by auth config)"],
        ],
    )
    T.add_body(doc, "The first imports (2 titles on 09 Oct) proved the enrich-vs-import branch; later runs "
                    "correctly de-duplicate everything already present, demonstrating idempotent ingestion.")

    T.add_subheading(doc, "6. JUnit Test Suite")
    T.add_table_label(doc, "Table 8: Automated Test Execution (mvn test - 10 tests, 0 failures)")
    T.add_table(
        doc,
        ["Test class", "Tests", "Assertion demonstrated", "Result"],
        [
            ["CirculationConcurrencyTest", "1", "20 threads race for one copy: exactly 1 loan, 19 conflicts", "PASS"],
            ["SnowflakeIdGeneratorTest", "2", "50,000 IDs / 20 threads, 0 collisions; monotonic k-order", "PASS"],
            ["IdempotentPaymentTest", "2", "Replayed idempotency key returns identical receipt, no double charge", "PASS"],
            ["ConsistentHashRingTest", "2", "3 -> 4 node churn 28.3% vs 75.1% modulo; distribution balance", "PASS"],
            ["PrefixTrieTest", "1", "Prefix matching, ranking and case-insensitive suggestions", "PASS"],
            ["TokenBucketRateLimiterTest", "1", "Capacity exhaustion, burst allowance, refill behaviour", "PASS"],
            ["Base62EncoderTest", "1", "Bijective encode/decode incl. boundary values", "PASS"],
            ["Total", "10", "BUILD SUCCESS", "0 failures"],
        ],
    )

    T.add_subheading(doc, "7. End-to-End Workflow Verification (verify_workflows.py)")
    T.add_table_label(doc, "Table 9: Fourteen-Step Live Workflow Verification (14/14 PASS)")
    T.add_table(
        doc,
        ["Step", "Workflow", "Key evidence", "Status"],
        [
            ["1", "Member registration", "MEM-... created with hashed shard key", "PASS"],
            ["2", "Member login (JWT)", "Token issued and accepted", "PASS"],
            ["3", "Authenticated identity (/me)", "Identity verified from bearer token", "PASS"],
            ["4", "Book listing & pagination", "Paged catalogue returned", "PASS"],
            ["5", "Search via indexed query", "Matches returned for probe query", "PASS"],
            ["6", "Book details & availability", "Copy-level availability tracked", "PASS"],
            ["7", "Borrow copy", "Loan created with due date", "PASS"],
            ["8", "Active loans", "Loan visible in member account", "PASS"],
            ["9", "Return copy", "Copy back to AVAILABLE, loan RETURNED", "PASS"],
            ["10", "Duplicate return defence", "ALREADY_RETURNED (idempotent, no state corruption)", "PASS"],
            ["11", "Reservation queue", "FIFO position assigned", "PASS"],
            ["12", "Idempotent payment", "Replay of same idempotency key confirmed safe", "PASS"],
            ["13", "10-thread borrow race", "1 success, 9 conflicts - zero double borrowing", "PASS"],
            ["14", "Renewal cap + wait-list", "+14 d extension, max 2 enforced, 409 on wait-list", "PASS"],
        ],
    )

    T.add_subheading(doc, "8. Working Prototype Screenshots")
    shots = [
        ("r01_catalogue_guest.png", "Public catalogue (guest) - 35 titles, live availability badges and Open "
                                    "Library covers."),
        ("r02_autocomplete.png", "Prefix-trie autocomplete dropdown with measured latency badge."),
        ("r03_search_results.png", "Catalogue search for 'dist' through the optimised indexed path (7 results, "
                                   "25 ms badge)."),
        ("r04_auth_modal.png", "Authentication modal - the entry point protected by the strict rate-limit bucket."),
        ("r05_catalogue_member.png", "Catalogue as a signed-in member."),
        ("r06_book_detail_modal.png", "Book detail - availability, physical barcode inventory (one RESERVED_FOR_PICKUP, "
                                      "one BORROWED, one AVAILABLE) and the Base62 shareable link."),
        ("r07_my_library.png", "My Library - active loans with due dates plus the reservation queue."),
        ("r08_fines_unpaid.png", "Fines & Payments - Rs.24.00 outstanding fine (16 days x Rs.1.50/day) and settled "
                                 "receipt history."),
        ("r09_payment_modal.png", "Idempotency-protected payment confirmation (UUID token shown)."),
        ("r10_payment_receipt.png", "Settled account after payment - receipt with transaction reference."),
        ("r11_systemdesign_lab_top.png", "System Design Lab telemetry - rate limiter counters (28 rejected), "
                                         "cache-aside hits, Snowflake counter, catalogue statistics."),
        ("r12_systemdesign_hash_ring.png", "Consistent-hash simulator run live: 28.1% churn vs 75.1% modulo on "
                                           "1000 keys / 100 vnodes."),
        ("r13_systemdesign_rate_limiter.png", "Token-bucket counters after the injected login burst: 50 permitted, "
                                              "28 throttled (HTTP 429)."),
        ("r14_admin_operations.png", "Librarian/admin operations desk - catalogue entry, copy allocation, crawler "
                                     "console."),
        ("r15_swagger_api.png", "Swagger / OpenAPI reference for the Vidya LMS REST API."),
        ("r17_systemdesign_trie.png", "Prefix-trie benchmark run live: Precision@5 46.0%, Recall@5 80.0% over 5 "
                                      "academic queries."),
        ("r18_systemdesign_caching.png", "Cache performance comparator - baseline vs optimised architecture with "
                                         "live throughput delta."),
    ]
    for fname, cap in shots:
        add_real_figure(doc, os.path.join(SHOTS, fname), cap, width=6.0)

    # ---------------- 8. Analysis ------------------------------------------ #
    T.add_section(doc, "Analysis")

    T.add_analysis_heading(doc, "1. How does the system behave as concurrency scales from 10 to 100 users?")
    T.add_analysis_step(doc, "Throughput grew from 313.10 to 507.85 req/s (62% more) while average latency grew "
                             "from 30.08 to 113.29 ms - the classic saturation curve. Median latency stayed far "
                             "below P95 (110 vs 196 ms at High), proving the tail comes from queueing, not errors. "
                             "The 0.00% error rate at every tier shows Tomcat, the Mongo pool and Redis handle the "
                             "burst without connection resets.")

    T.add_analysis_heading(doc, "2. What happens to cached keys when the server cluster grows?")
    T.add_analysis_step(doc, "Adding a 4th node remapped only 28.3% of 2,000 keys under consistent hashing versus "
                             "75.1% under modulo hashing (theoretical 25% / 75%). In production this preserves "
                             "roughly 72% of warm cache entries, preventing a cache stampede against MongoDB - the "
                             "primary reason the ring is used for request routing.")

    T.add_analysis_heading(doc, "3. Can two members borrow the last physical copy simultaneously?")
    T.add_analysis_step(doc, "No. The 20-thread race test and the 10-thread workflow probe both produced exactly "
                             "one loan and N-1 HTTP 409 conflicts. Because the AVAILABLE -> BORROWED transition is "
                             "a single-document atomic findAndModify, the database - not application locking - "
                             "serialises the claim, so the guarantee holds across multiple service instances.")

    T.add_analysis_heading(doc, "4. What happens when Redis becomes unavailable?")
    T.add_analysis_step(doc, "Rate limiting degrades to thread-safe in-memory buckets and reads fall back to "
                             "MongoDB directly; the API continues serving traffic without 5xx errors. The trade-off "
                             "is lost cross-instance consistency of counters and higher database read load - "
                             "acceptable for availability (AP) but noted as a limitation.")

    T.add_analysis_heading(doc, "5. Trade-offs and bottlenecks observed")
    T.add_analysis_step(doc, "1) MongoDB atomic operators replaced distributed locks: cheaper and race-free for "
                             "single documents, but multi-collection cross-shard transactions would need sagas.")
    T.add_analysis_step(doc, "2) The in-memory trie is sub-millisecond with zero infrastructure, but costs JVM "
                             "heap (about 15 MB per 50k titles) and rebuilds at startup.")
    T.add_analysis_step(doc, "3) Snowflake blocks on clock rollback to stay collision-free; the 12-bit sequence "
                             "caps each worker at 4,096 IDs/ms, requiring new workers beyond that.")
    T.add_analysis_step(doc, "4) The baseline-vs-optimised search gap is small at 35 titles because in-memory "
                             "scanning is cheap; the indexed + cached path is expected to dominate as the "
                             "catalogue grows - the honest result is reported rather than inflated.")

    # ---------------- 9. API / Command Reference & Test Results ------------ #
    T.add_section(doc, "API / Command Reference & Test Results")
    T.add_body(doc, "The REST surface is fully documented in Swagger UI (see Figure 30). Principal endpoints:")
    T.add_table_label(doc, "Table 10: Principal REST Endpoints")
    T.add_table(
        doc,
        ["Method & path", "Purpose", "Component demonstrated"],
        [
            ["POST /api/auth/register, /api/auth/login", "Registration and JWT login", "Rate Limiter (strict bucket), Snowflake ID"],
            ["GET /api/books, /api/books/{id}", "Paged catalogue, detail with availability", "Sharding keys, Redis cache-aside"],
            ["GET /api/books/search?q=&mode=", "Indexed vs baseline search", "KV store, search cache"],
            ["GET /api/autocomplete?prefix=", "Top-k ranked suggestions", "Prefix trie"],
            ["GET /api/autocomplete/evaluate?k=5", "Precision@5 / Recall@5 evaluation", "Autocomplete accuracy"],
            ["POST /api/circulation/borrow, /return, /renew", "Atomic claim, return, renewal cap", "Consistent circulation, CAS renewals"],
            ["POST /api/reservations, GET /api/reservations/my-reservations", "Reservation queue", "Wait-list interlock"],
            ["GET /api/fines/my-fines, POST /api/payments", "Outstanding fines and idempotent settlement", "Idempotency key + receipt replay"],
            ["POST /api/urls/shorten, GET /s/{code}", "Shareable short links with 302 redirect", "Snowflake + Base62 shortener"],
            ["POST /api/crawler/trigger, GET /api/crawler/jobs", "Async publisher ingestion with ISBN dedup", "Web crawler pipeline"],
            ["GET /api/consistenthash/ring, POST /api/consistenthash/simulate", "Ring state and churn benchmark", "Consistent hashing"],
            ["GET /api/sharding/stats", "Declared shard keys per collection", "Sharding configuration"],
            ["GET /api/metrics/summary", "Live counters (limiter, cache, IDs, catalogue)", "Observability"],
        ],
    )
    T.add_subheading(doc, "Command Reference")
    T.add_code(doc,
               "# Start the stack (Windows)\n"
               "Start-Service -Name MongoDB                    # MongoDB :27017\n"
               "docker start library-redis                     # Redis\n"
               "cd backend  && java -jar target\\scalable-library-system-1.0.0.jar\n"
               "cd frontend && npm run dev                     # UI on :5173\n"
               "\n"
               "# Evidence regeneration\n"
               "python benchmarks\\run_benchmarks.py            # workload tiers, limiter, hashing, search\n"
               "python benchmarks\\verify_workflows.py          # 14-step end-to-end suite\n"
               "python benchmarks\\prepare_demo_state.py        # seed an overdue fine for demo\n"
               "cd backend && mvn test                          # 10 JUnit tests")
    T.add_subheading(doc, "Test Results")
    T.add_body(doc, "mvn test summary (backend/target/surefire-reports): 10 tests executed, 0 failures, 0 errors - "
                    "BUILD SUCCESS. The full 14-step live verification suite (Table 9) also passed with zero "
                    "double-borrowing, correct renewal caps, and safe idempotent payment replay.")

    # ---------------- 10. Learning Outcomes -------------------------------- #
    T.add_section(doc, "Learning Outcomes")
    T.add_bullet(doc, "Mapped and justified all eight system-design components from measured bottlenecks - the "
                      "rate limiter rejecting 80% of an abusive login burst, consistent hashing churning only "
                      "28.3% of keys versus 75.1% under modulo hashing, and cache-aside reads reaching a 98.9% "
                      "search-cache hit ratio.")
    T.add_bullet(doc, "Implemented and measured the components end-to-end: Redis-Lua token bucket with in-memory "
                      "fallback, 160-vnode hash ring, 50,000 collision-free Snowflake IDs over 20 threads, Base62 "
                      "shortener (mean 5.10 ms), asynchronous crawler (90.9 books/s) and the ranked prefix trie "
                      "(P@5 0.46 / R@5 0.80 in 2.32 ms).")
    T.add_bullet(doc, "Verified correctness and concurrency with 10/10 JUnit tests and 14/14 live workflow checks - "
                      "zero double-borrowing under race tests and idempotent payments - while reporting trade-offs "
                      "honestly, including the modest 1.08x small-catalogue search gain that widens at scale.")
    T.add_bullet(doc, "Integrated the React 18 / TypeScript frontend with a live System Design Lab that visualises "
                      "every component's telemetry, and demonstrated the complete prototype in the browser.")

    os.makedirs(os.path.dirname(OUT_DOCX), exist_ok=True)
    saved = _save_with_retry(doc, OUT_DOCX)
    print("Report saved: %s" % saved)
    print("Figures placed: %d (image slots + embedded charts/screenshots)" % FIG["n"])


def _save_with_retry(doc, target, attempts=15, delay=2.0):
    """Save via a temp file then move into place with retries.

    Windows occasionally locks freshly-written files (Defender/AV scans) and
    Word locks any document that is open; both raise PermissionError.
    """
    import shutil
    import time
    tmp = target + ".tmp"
    doc.save(tmp)
    for _ in range(attempts):
        try:
            os.replace(tmp, target)
            return target
        except PermissionError:
            time.sleep(delay)
    fallback = target.replace(".docx", "_UPDATED.docx")
    os.replace(tmp, fallback)
    print("NOTE: %s was locked (open in Word?). Updated copy written to:\n  %s"
          % (target, fallback))
    return fallback


if __name__ == "__main__":
    build_report()
