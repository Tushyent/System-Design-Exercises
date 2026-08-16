# Screenshot Plan — where every image in `SD_A6.docx` comes from

The report (`scripts/gen_report.py`) has a fixed list of **figure slots** under
`assets/images/`. Every slot is inserted automatically and numbered
`Figure N` **only if the file exists** when the report is generated. Five slots
are charts that are generated from the observed data (`fig_architecture`,
`fig_normal`, `fig_burst`, `fig_drain`, `fig_concurrent`); the rest are
**real screenshots / terminal captures** that prove the live run.

Workflow: capture the screenshots below (browser automation or terminal), save
them under `assets/images/` **with the exact file names**, then re-run:

```powershell
python scripts/gen_report.py     # regenerates SD_A6.docx and embeds them
```

## 1. Slot reference

| Slot file | Report section | What to capture | How |
|-----------|----------------|-----------------|-----|
| `fig_swagger.png` | 0. Startup, Swagger | Swagger UI showing the `/api` endpoints | Browser: `http://localhost:8080/swagger-ui.html` |
| `fig_hello_200.png` | 1. Normal traffic | HTTP 200 JSON from `GET /api/hello?clientId=demo` (shows `remaining`) | Browser after `DELETE /api/bucket/demo` |
| `fig_429_error.png` | 2. Burst traffic | HTTP 429 JSON rendered in the browser for an exhausted client | Browser after draining the bucket |
| `fig_startup.png` | 0. Startup | Spring Boot startup log: Tomcat on **8080** + Redis connection | Terminal: `.\run_lab.ps1` (log also in `app.log`) |
| `fig_mvn_test.png` | 0. Startup | `mvn test` summary — `Tests run: 33, Failures: 0` | Terminal: `mvn test` |
| `fig_concurrent_json.png` | 4. Concurrent | Concurrency-test JSON (`allowed`, `rejected`, `total`) | Terminal: `python scripts/load_test.py concurrent --client demo` |
| `fig_429_headers.png` | 5. 429 + Redis | `curl -i` output: `HTTP/1.1 429` + `Retry-After: 1` header | Terminal: `curl -i` after drain |
| `fig_hgetall.png` | 5. 429 + Redis | `redis-cli HGETALL ratelimit:<clientId>` → `{tokens, ts}` | Terminal: `docker exec` into the Redis container |

Notes:

- **Capacity / refill** used everywhere: `10` tokens, `5` tokens/s (the defaults).
- Capture browser screenshots at 1440px+ wide so tables/JSON are not clipped.
- PNG only; the report will not embed other formats.

## 2. Prepare the app (both capture methods)

```powershell
docker compose up -d redis            # 1. Redis on localhost:6379
mvn spring-boot:run                   # 2. app on localhost:8080 (keep this shell open)
```

Or the one-shot orchestration: `.\run_lab.ps1` (starts Redis, runs all demos,
writes the report). For screenshots you generally want the app *staying up*, so
prefer the two-shell manual start above.

## 3. Master prompt (paste into Gemini / Antigravity)

Give your agent this single prompt. It captures every browser slot with
**AgentBrowser/Playwright** and every terminal slot with its shell tool:

```text
You are helping produce evidence screenshots for a Spring Boot token-bucket
rate-limiter lab report. The app is already running at http://localhost:8080
(Redis on localhost:6379, capacity=10, refill=5 tokens/s).

Use browser automation (AgentBrowser or Playwright) to capture and save PNG
screenshots into <workspace>/ex-6-rate-limiter/assets/images/ with EXACTLY
these names. Use the shell to run the terminal commands and save their output
text where noted.

1. fig_swagger.png  — open http://localhost:8080/swagger-ui.html, wait for the
   endpoint list, full-page screenshot of the Swagger UI showing GET /api/hello,
   GET/DELETE /api/bucket/{clientId}, POST /api/test/concurrent.
2. First reset the client: run  curl -X DELETE http://localhost:8080/api/bucket/demo
   Then open http://localhost:8080/api/hello?clientId=demo and screenshot the
   HTTP 200 JSON response (it contains "remaining" tokens) as fig_hello_200.png.
3. Run  python scripts/load_test.py drain --client demo   (working directory:
   <workspace>/ex-6-rate-limiter). Then open
   http://localhost:8080/api/hello?clientId=demo again and screenshot the HTTP
   429 JSON error page as fig_429_error.png.
4. Terminal slots (run in the shell and save as text/terminal captures):
   - fig_mvn_test.png  — run "mvn test" and capture the summary line
     "Tests run: 33, Failures: 0, Errors: 0, Skipped: 0".
   - fig_concurrent_json.png — run
     "python scripts/load_test.py concurrent --client demo" and capture the
     returned JSON (allowed=10, rejected=390, total=400).
   - fig_429_headers.png — after step 3 run
     "curl -i http://localhost:8080/api/hello?clientId=demo" and capture the
     headers block (HTTP/1.1 429, Retry-After: 1).
   - fig_hgetall.png — run
     "docker exec <redis-container> redis-cli HGETALL ratelimit:demo" and
     capture the output showing tokens and ts fields.
5. fig_startup.png — capture the Spring Boot startup log lines showing "Tomcat
   started on port 8080" and the Redis connection (from app.log or the app
   console).

Do NOT modify any source code. Only capture and save the images under
assets/images/ with the exact file names. When done, list the files you saved.
```

## 4. Per-slot prompts (when you want only one image)

**Browser slots** (AgentBrowser / Playwright):

```text
Open http://localhost:8080/swagger-ui.html, wait for the API documentation to
load, and save a full-page screenshot to
<workspace>/ex-6-rate-limiter/assets/images/fig_swagger.png
```

```text
Run  curl -X DELETE http://localhost:8080/api/bucket/demo  then open
http://localhost:8080/api/hello?clientId=demo in the browser and save a
screenshot of the HTTP 200 JSON response to
<workspace>/ex-6-rate-limiter/assets/images/fig_hello_200.png
```

```text
Run  python scripts/load_test.py drain --client demo  (from the
ex-6-rate-limiter folder) then open
http://localhost:8080/api/hello?clientId=demo and save a screenshot of the
HTTP 429 JSON response to
<workspace>/ex-6-rate-limiter/assets/images/fig_429_error.png
```

**Terminal slots** (agent shell / manual):

```text
Run "mvn test" in <workspace>/ex-6-rate-limiter and save the terminal capture
showing "Tests run: 33, Failures: 0, Errors: 0, Skipped: 0" to
assets/images/fig_mvn_test.png
```

```text
Run "python scripts/load_test.py concurrent --client demo" in
<workspace>/ex-6-rate-limiter and save the returned JSON (allowed=10,
rejected=390, total=400, durationMillis~70) to assets/images/fig_concurrent_json.png
```

```text
After draining the bucket, run
"curl -i http://localhost:8080/api/hello?clientId=demo" and save the headers
(HTTP/1.1 429, Retry-After: 1, X-RateLimit-Remaining: 0) to
assets/images/fig_429_headers.png
```

```text
Run "docker ps" to find the Redis container, then
"docker exec <container> redis-cli HGETALL ratelimit:demo" and save the output
(hash with "tokens" and "ts" fields) to assets/images/fig_hgetall.png
```

```text
Capture the Spring Boot startup log lines "Tomcat started on port 8080" and
the Redis connection message and save to assets/images/fig_startup.png
```

## 5. Automated capture with Playwright

A helper script automates the three browser slots and prints the terminal
commands/prompts for the rest:

```powershell
pip install playwright
python -m playwright install chromium
python scripts/capture_screenshots.py --mode browser   # captures swagger/hello_200/429_error
python scripts/capture_screenshots.py --mode prompts   # prints all prompts above
```

## 6. Verification after capturing

```powershell
python scripts/gen_report.py
```

The generator prints the images it embedded. Then open `SD_A6.docx` and confirm
the screenshot sections show real evidence:

- [ ] Swagger UI screenshot appears in section 0.
- [ ] `mvn test` summary (33 / 0 / 0) and startup log appear in section 0.
- [ ] HTTP 200 JSON appears in section 1; HTTP 429 page in section 2.
- [ ] Concurrent JSON appears in section 4.
- [ ] `curl -i` 429 headers and `redis-cli HGETALL` appear in section 5.