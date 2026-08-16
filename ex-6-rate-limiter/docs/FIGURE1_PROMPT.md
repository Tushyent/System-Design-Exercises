# Figure 1 — Architecture Diagram: generate with Gemini

The report currently renders `fig_architecture.png` with matplotlib. If you want a
better-looking diagram, generate it with Gemini using the prompt below, save the
result as `assets/images/fig_architecture.png` (replacing the generated chart),
and re-run `python scripts/gen_report.py`.

Do NOT change the file name: the report only embeds `fig_architecture.png`.

---

## Prompt to paste into Gemini

```text
Create a clean, professional, flat-vector architecture diagram for a university
"API Rate Limiter (Token Bucket)" lab report. White background, wide landscape
composition (roughly 16:9), high resolution, crisp thin borders, sans-serif
font (Arial/Inter), NO watermark, NO logo, no extra decorations. Every text
label below must be spelled EXACTLY as given.

LAYOUT (top-to-bottom, left-to-right):

TOP ROW (three rounded rectangles, all equal height, aligned vertically):
1. Left box  "Clients"
   - sub-lines: "identified by X-Client-Id header,"
                "clientId query param, or IP"
                "e.g. demo-user"
   - fill: #EEF2FF, border: #4F46E5 (indigo)
2. Centre box "Spring Boot App"
   - sub-lines: "(any number of application instances)"
                "ApiController -> RateLimiterService"
                "-> RedisBucketStore (EVAL Lua)"
   - fill: #EEF2FF, border: #4F46E5 (indigo)
3. Right box "HTTP Response"
   - sub-lines: "200 OK (allowed)"
                "429 Too Many Requests + Retry-After"
   - fill: #EEF2FF, border: #4F46E5 (indigo)

BOTTOM ROW (two rounded rectangles):
4. Large centre box "Redis 7  (shared, centralized)"
   - sub-lines:
     "key ratelimit:<clientId>  ->  hash {tokens, ts}"
     "atomic Lua: read -> refill -> consume/write -> expire"
     "one EVAL serializes concurrent requests"
     "across all instances (no token double-spend)"
   - fill: #ECFDF5, border: #059669 (green)
5. Left box "Tests"
   - sub-lines: "mvn test"
                "33 unit + integration tests"
   - fill: #FFFBEB, border: #D97706 (amber)

ARROWS (solid, with a filled arrowhead, colour #334155 slate except the Redis
ones which are green #059669):
- Arrow 1 (horizontal, top row): Clients -> Spring Boot App, label "requests"
  placed just above the arrow.
- Arrow 2 (horizontal, top row): Spring Boot App -> HTTP Response, label
  "decision" placed just above the arrow.
- Arrow 3 (vertical, green, top-centre down): Spring Boot App -> Redis 7,
  label "EVAL token_bucket.lua" placed beside the arrow.
- Arrow 4 (vertical, green, bottom-up): Redis 7 -> Spring Boot App (return
  arrow, same line as Arrow 3, opposite direction).
- Arrow 5 (horizontal, bottom row): Tests -> Redis 7, label "verify" placed
  above the arrow.

STYLE:
- rounded-rectangle boxes, consistent padding, labels centred in each box
- title text bold and slightly larger than sub-lines; sub-lines smaller and
  light grey (#334155 / #374151)
- arrows neat, right angles preferred, no crossing lines
- balanced whitespace; the diagram must be easily readable at report width
  (~6 inches)
```

## Notes

- Paste the prompt as-is into Gemini (or your Antigravity agent) and ask for a
  single wide PNG.
- If the text comes out garbled, ask Gemini to render **only** English text and
  to re-check every label, then iterate.
- Recommended output: ~2400 x 1350 px (16:9), transparent or white background.
- Save exactly to `assets/images/fig_architecture.png`, then:
  `python scripts/gen_report.py`