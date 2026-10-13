"""Write dashboard part 1: head + css + header + crawl/queue panels."""
BODY1 = """<div class="wrap">
<header class="nav">
<div><h1>Job Portal <span>- Discovery Crawler Dashboard</span></h1>
<p>Spring Boot REST API | Token Bucket 5 tokens 1/s | Redis Queue + Visited Set</p></div>
<div class="links">
<a href="/swagger-ui.html" target="_blank">Swagger</a>
<a href="/api-docs" target="_blank">API Docs</a>
<a href="/mock/jobs" target="_blank">Mock Job Site</a>
<a href="/api/mock-urls" target="_blank">Seeds JSON</a>
</div>
</header>
<div class="how">
<b>How to start (30 seconds):</b>
1. Keep <b>Seed URL</b> as the mock hub (works offline) <b>OR</b> pick a real company below.
2. Press <b>Crawl Seed</b> -- watch tiles 2 and lists 3 fill.
3. Press <b>Burst 8x</b> -- watch 5 green 200s then red 429s in panel 4.
Fresh run? Press <b>Reset All</b> first.
</div>
<div class="grid">
<div>
<section class="card">
<h2>1. Crawl - POST /api/crawl (max 10 pages)</h2>
<label>Seed URL (job listing page)</label>
<input id="seed" value="http://localhost:8080/mock/jobs">
<label>Client ID (X-Client-Id header)</label>
<input id="client" value="demo-client">
<div class="row">
<button onclick="doCrawl()">Crawl Seed</button>
<button class="ghost" onclick="resetAll()">Reset All</button>
</div>
<label>Mock seeds (always work, offline)</label>
<div class="row">
<button class="ghost" onclick="quickSeed('/mock/jobs')">Mock Hub</button>
<button class="ghost" onclick="quickSeed('/mock/jobs/category/engineering')">Mock Engg</button>
<button class="ghost" onclick="quickSeed('/mock/jobs/category/design')">Mock Design</button>
</div>
<label>Real company career pages (need internet; JS-heavy sites may yield 0 links)</label>
<div class="row" id="ext-seeds"></div>
<div class="stats">
<div class="stat"><div class="v" id="s-crawled" style="color:var(--blue)">-</div><div class="l">Pages crawled</div></div>
<div class="stat"><div class="v" id="s-visited" style="color:var(--green)">-</div><div class="l">Visited total</div></div>
<div class="stat"><div class="v" id="s-queue" style="color:var(--amber)">-</div><div class="l">Queue left</div></div>
</div>
<div class="row">
<button class="warn" onclick="refreshState()">Refresh State</button>
</div>
</section>
<section class="card">
<h2 class="green">2. Queue FIFO + Visited Set</h2>
<div class="row">
<button class="ghost" onclick="showQueue()">Queue</button>
<button class="ghost" onclick="showVisited()">Visited</button>
</div>
<ul class="list" id="qv-list"><li class="pending">Press Refresh to load state...</li></ul>
</section>
</div>
<div>
<section class="card">
<h2 class="amber">3. Token Bucket - 5 tokens, refill 1/s</h2>
<label>Bucket client</label>
<input id="bucket-client" value="burst-client">
<div class="tokens" id="tokens"></div>
<div style="font-size:.78rem;color:var(--muted)" id="bucket-info">Press Refresh...</div>
<div class="row">
<button class="warn" onclick="burst()">Burst 8x (5x200 then 429)</button>
<button class="ghost" onclick="resetBucket()">Reset Bucket</button>
</div>
</section>
<section class="card">
<h2 class="purple">4. Burst Results + Last Crawl</h2>
<table><thead><tr><th>#</th><th>Status</th><th>Detail</th></tr></thead>
<tbody id="burst-rows"><tr><td colspan="3">No burst run yet.</td></tr></tbody></table>
<div id="crawl-summary" style="font-size:.82rem;color:var(--muted);margin-top:8px">No crawl yet.</div>
<ul class="list" id="crawled-list"></ul>
</section>
</div>
</div>
<section class="card">
<h2>5. Request Log</h2>
<div id="log">Dashboard ready. App http://localhost:8080 | Redis localhost:6379
</div>
</section>
<section class="card">
<h2>6. Architecture flow</h2>
<div class="arch">Client/Postman --POST /api/crawl {seedUrl}--&gt; Spring Boot API --Lua EVAL--&gt; Redis ratelimit:{client} (5 cap, 1/s)
  allowed: BFS crawl max 10 --RPUSH/LPOP--&gt; Redis list jobcrawl:queue | SADD --&gt; Redis set jobcrawl:visited
  fetch+parse --&gt; Job Website (mock /mock/jobs or real careers/Workday pages) | empty bucket: 429 + Retry-After</div>
<details><summary>Q(c) analysis</summary>
<p>Queue: FIFO list, seed first, 10 pages per request BFS. Visited: canonicalised set, repeats skipped.
Bucket: 5-token burst, 1/s refill, atomic Lua, per-client, 429 + Retry-After.
Redis: shared buckets + queue + visited for all clients.</p></details>
</section>
<footer>Job Portal Crawler | System Design Lab | cap 5 | refill 1/s | max 10 pages</footer>
</div>
"""

with open("scripts/dash_body1.html", "w", encoding="utf-8") as f:
    f.write(BODY1)
print("body1 written", len(BODY1))
