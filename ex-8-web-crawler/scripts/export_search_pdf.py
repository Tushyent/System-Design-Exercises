#!/usr/bin/env python3
"""
Web Crawler - Automated Search Summary PDF Report Generator
UCS3513 System Design Laboratory - Sri Sivasubramaniya Nadar College of Engineering

Generates a publication-grade PDF report documenting full-text search results,
ranking breakdown (Composite, BM25, Google PageRank), and line-by-line occurrence
specifications (Content-Title, Content Line Numbers, Metadata).
"""

import sys
import os
import json
import argparse
from datetime import datetime

try:
    import fitz  # PyMuPDF
except ImportError:
    print("PyMuPDF (fitz) is not installed. Run: pip install pymupdf", file=sys.stderr)
    sys.exit(1)

try:
    import requests
except ImportError:
    import urllib.request
    requests = None


def fetch_search_results(base_url, query, sort_by="COMPOSITE", category=""):
    url = f"{base_url}/api/crawler/search?q={urllib.parse.quote(query)}&sortBy={urllib.parse.quote(sort_by)}"
    if category:
        url += f"&category={urllib.parse.quote(category)}"
    
    if requests:
        resp = requests.get(url, timeout=10)
        resp.raise_for_status()
        return resp.json()
    else:
        req = urllib.request.Request(url, headers={"User-Agent": "WebCrawler-ReportGen/1.0"})
        with urllib.request.urlopen(req, timeout=10) as r:
            return json.loads(r.read().decode("utf-8"))


def generate_search_pdf(data, output_path):
    doc = fitz.open()
    
    # Page dimensions (A4 in points: 595.32 x 841.92)
    PAGE_WIDTH = 595.32
    PAGE_HEIGHT = 841.92
    MARGIN_LEFT = 40.0
    MARGIN_RIGHT = 40.0
    CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
    
    # Color Palette
    C_NAVY = (15/255, 23/255, 42/255)       # #0f172a
    C_BLUE = (14/255, 165/255, 233/255)     # #0ea5e9
    C_DARK = (30/255, 41/255, 59/255)       # #1e293b
    C_TEXT = (51/255, 65/255, 85/255)       # #334155
    C_MUTED = (100/255, 116/255, 139/255)   # #64748b
    C_LIGHT_BG = (248/255, 250/255, 252/255)# #f8fafc
    C_BORDER = (226/255, 232/255, 240/255)  # #e2e8f0
    C_GREEN = (16/255, 185/255, 129/255)    # #10b981
    C_AMBER = (217/255, 119/255, 6/255)     # #d97706
    C_PURPLE = (147/255, 51/255, 234/255)   # #9333ea
    
    page = doc.new_page(width=PAGE_WIDTH, height=PAGE_HEIGHT)
    
    # ---------- 1. Top Header Banner ----------
    header_rect = fitz.Rect(0, 0, PAGE_WIDTH, 85)
    page.draw_rect(header_rect, color=None, fill=C_NAVY)
    
    # Accent top border
    accent_bar = fitz.Rect(0, 0, PAGE_WIDTH, 4)
    page.draw_rect(accent_bar, color=None, fill=C_BLUE)
    
    page.insert_text(fitz.Point(MARGIN_LEFT, 32), "WEB CRAWLER - SEARCH & CONTENT DISCOVERY REPORT",
                     fontsize=13, fontname="helv", color=(1, 1, 1), fontfile=None)
    page.insert_text(fitz.Point(MARGIN_LEFT, 50), "UCS3513 System Design Laboratory | Sri Sivasubramaniya Nadar College of Engineering",
                     fontsize=8.5, fontname="helv", color=(186/255, 230/255, 253/255))
    
    gen_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    page.insert_text(fitz.Point(MARGIN_LEFT, 68), f"Generated: {gen_time}  |  Frontier Scheduling: Redis FIFO  |  IR Engine: Okapi BM25 + Google PageRank",
                     fontsize=7.5, fontname="helv", color=(148/255, 163/255, 184/255))
    
    y = 100.0
    
    # ---------- 2. Executive Query Summary Box ----------
    query_box = fitz.Rect(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + 78)
    page.draw_rect(query_box, color=C_BORDER, fill=C_LIGHT_BG, width=1)
    
    # Decorative left accent bar on query box
    q_bar = fitz.Rect(MARGIN_LEFT, y, MARGIN_LEFT + 4, y + 78)
    page.draw_rect(q_bar, color=None, fill=C_BLUE)
    
    query = data.get("query", "")
    total_matches = data.get("totalMatches", 0)
    total_mentions = data.get("totalOccurrencesInCorpus", 0)
    search_time = data.get("searchTimeMs", 0)
    ranking_mode = data.get("rankingMode", "COMPOSITE")
    
    page.insert_text(fitz.Point(MARGIN_LEFT + 14, y + 18), "SEARCH QUERY ANALYSIS", fontsize=8.5, fontname="helv", color=C_BLUE)
    page.insert_text(fitz.Point(MARGIN_LEFT + 14, y + 36), f"Search Keyword:  '{query}'", fontsize=11, fontname="helv", color=C_NAVY)
    
    stats_line1 = f"Total Matched Documents: {total_matches}    |    Corpus Mentions: {total_mentions}x    |    Latency: {search_time} ms"
    page.insert_text(fitz.Point(MARGIN_LEFT + 14, y + 54), stats_line1, fontsize=8.5, fontname="helv", color=C_TEXT)
    
    cat_dist = data.get("categoryDistribution", {})
    cat_str = ", ".join([f"{k} ({v})" for k, v in cat_dist.items()]) if cat_dist else "None"
    stats_line2 = f"Ranking Strategy: {ranking_mode}    |    Discovered Topics: {cat_str}"
    page.insert_text(fitz.Point(MARGIN_LEFT + 14, y + 70), stats_line2, fontsize=7.5, fontname="helv", color=C_MUTED)
    
    y += 94.0
    
    # ---------- 3. Section Title: Document Results ----------
    page.insert_text(fitz.Point(MARGIN_LEFT, y), f"Ranked Content Matches & Line Location Breakdown ({total_matches} Documents Discovered)",
                     fontsize=10.5, fontname="helv", color=C_NAVY)
    y += 14.0
    
    # Underline
    page.draw_line(fitz.Point(MARGIN_LEFT, y), fitz.Point(PAGE_WIDTH - MARGIN_RIGHT, y), color=C_BORDER, width=1)
    y += 12.0
    
    results = data.get("results", [])
    if not results:
        page.insert_text(fitz.Point(MARGIN_LEFT, y + 20), f"No matching documents discovered for query '{query}'.",
                         fontsize=9, fontname="helv", color=C_MUTED)
        doc.save(output_path)
        doc.close()
        return output_path

    # Iterate results
    for r in results:
        rank = r.get("rank", 1)
        title = r.get("title", "(No Title)")
        url = r.get("url", "")
        rel_score = r.get("relevanceScore", 0.0)
        bm25_score = r.get("bm25Score", 0.0)
        pr_score = r.get("pageRankScore", 0.0)
        category = r.get("category", "General")
        word_count = r.get("wordCount", 0)
        depth = r.get("depth", 0)
        occurrences = r.get("occurrences", [])
        
        # Estimate card height
        occ_count = min(len(occurrences), 6)
        card_height = 54.0 + (occ_count * 18.0) if occ_count > 0 else 60.0
        
        # Check if page break needed
        if y + card_height > PAGE_HEIGHT - 50:
            # Draw footer on current page before moving
            draw_footer(page, doc.page_count, query)
            page = doc.new_page(width=PAGE_WIDTH, height=PAGE_HEIGHT)
            y = 40.0
        
        # Draw card container
        card_rect = fitz.Rect(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + card_height)
        page.draw_rect(card_rect, color=C_BORDER, fill=C_LIGHT_BG, width=0.8)
        
        # Rank pill
        rank_bg = C_NAVY if rank > 3 else (C_AMBER if rank == 1 else (C_DARK if rank == 2 else C_BLUE))
        rank_pill = fitz.Rect(MARGIN_LEFT + 8, y + 8, MARGIN_LEFT + 32, y + 23)
        page.draw_rect(rank_pill, color=None, fill=rank_bg)
        page.insert_text(fitz.Point(MARGIN_LEFT + 13, y + 19), f"#{rank}", fontsize=8, fontname="helv", color=(1, 1, 1))
        
        # Title (truncate if too long)
        title_disp = (title[:68] + "...") if len(title) > 70 else title
        page.insert_text(fitz.Point(MARGIN_LEFT + 38, y + 19), title_disp, fontsize=9.5, fontname="helv", color=C_NAVY)
        
        # Scores block on right
        score_text = f"Score: {rel_score:.2f} | BM25: {bm25_score:.2f} | PR: {pr_score:.1f}"
        page.insert_text(fitz.Point(PAGE_WIDTH - MARGIN_RIGHT - 180, y + 19), score_text, fontsize=7.5, fontname="helv", color=C_BLUE)
        
        # URL line (with link annotation)
        url_disp = (url[:80] + "...") if len(url) > 82 else url
        page.insert_text(fitz.Point(MARGIN_LEFT + 8, y + 33), f"URL: {url_disp}", fontsize=7.5, fontname="helv", color=C_MUTED)
        link_rect = fitz.Rect(MARGIN_LEFT + 8, y + 25, PAGE_WIDTH - MARGIN_RIGHT - 8, y + 36)
        page.insert_link({"kind": fitz.LINK_URI, "from": link_rect, "uri": url})
        
        # Badges line: Category, Word Count, Depth
        meta_line = f"Category: {category}  |  Words: {word_count}  |  Crawl Depth: {depth}  |  Total Mentions: {r.get('matchOccurrences', 1)}x"
        page.insert_text(fitz.Point(MARGIN_LEFT + 8, y + 45), meta_line, fontsize=7.2, fontname="helv", color=C_TEXT)
        
        # Line Occurrences Section
        curr_occ_y = y + 56.0
        if occurrences:
            for occ in occurrences[:6]:
                loc_label = occ.get("locationLabel", "Content")
                raw_text = occ.get("rawText", occ.get("snippet", ""))
                # Clean html tags if present
                clean_text = raw_text.replace("<mark>", "[").replace("</mark>", "]")
                clean_disp = (clean_text[:72] + "...") if len(clean_text) > 75 else clean_text
                
                # Pill for location
                sec = occ.get("section", "content")
                pill_col = C_BLUE if sec == "content-title" else (C_AMBER if sec == "metadata" else C_GREEN)
                
                pill_rect = fitz.Rect(MARGIN_LEFT + 12, curr_occ_y - 8, MARGIN_LEFT + 95, curr_occ_y + 4)
                page.draw_rect(pill_rect, color=None, fill=pill_col)
                page.insert_text(fitz.Point(MARGIN_LEFT + 15, curr_occ_y), loc_label[:14], fontsize=6.5, fontname="helv", color=(1, 1, 1))
                
                # Line content
                page.insert_text(fitz.Point(MARGIN_LEFT + 102, curr_occ_y), clean_disp, fontsize=7.2, fontname="helv", color=C_TEXT)
                curr_occ_y += 16.0
        
        y += card_height + 10.0

    # Draw footer on the last page
    draw_footer(page, doc.page_count, query)
    
    # Save PDF
    doc.save(output_path)
    doc.close()
    return output_path


def draw_footer(page, page_num, query):
    PAGE_WIDTH = 595.32
    PAGE_HEIGHT = 841.92
    MARGIN_LEFT = 40.0
    MARGIN_RIGHT = 40.0
    C_BORDER = (226/255, 232/255, 240/255)
    C_MUTED = (148/255, 163/255, 184/255)
    
    # Line
    page.draw_line(fitz.Point(MARGIN_LEFT, PAGE_HEIGHT - 30),
                   fitz.Point(PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 30),
                   color=C_BORDER, width=0.8)
    
    # Footer texts
    page.insert_text(fitz.Point(MARGIN_LEFT, PAGE_HEIGHT - 18),
                     "UCS3513 System Design Lab - Automated Web Discovery Report",
                     fontsize=7, fontname="helv", color=C_MUTED)
    
    page.insert_text(fitz.Point(PAGE_WIDTH / 2 - 30, PAGE_HEIGHT - 18),
                     f"Query: '{query}'",
                     fontsize=7, fontname="helv", color=C_MUTED)
    
    page.insert_text(fitz.Point(PAGE_WIDTH - MARGIN_RIGHT - 45, PAGE_HEIGHT - 18),
                     f"Page {page_num}",
                     fontsize=7, fontname="helv", color=C_MUTED)


def main():
    parser = argparse.ArgumentParser(description="Export Web Crawler search summary as PDF")
    parser.add_argument("--query", "-q", default="quantum", help="Search keyword/term")
    parser.add_argument("--sort", "-s", default="COMPOSITE", help="Ranking mode (COMPOSITE, PAGERANK, BM25)")
    parser.add_argument("--category", "-c", default="", help="Optional category filter")
    parser.add_argument("--server", default="http://localhost:8080", help="Spring Boot server base URL")
    parser.add_argument("--out", "-o", default="", help="Output PDF file path")
    parser.add_argument("--json-input", help="Direct path to JSON file containing SearchResponse")
    args = parser.parse_args()

    clean_q = args.query.strip()
    if not clean_q:
        print("Error: query term cannot be empty.", file=sys.stderr)
        sys.exit(1)
        
    out_file = args.out
    if not out_file:
        safe_q = "".join([c if c.isalnum() else "_" for c in clean_q])
        out_file = f"search_report_{safe_q}.pdf"

    if args.json_input and os.path.exists(args.json_input):
        with open(args.json_input, "r", encoding="utf-8") as f:
            data = json.load(f)
    else:
        try:
            data = fetch_search_results(args.server, clean_q, args.sort, args.category)
        except Exception as e:
            print(f"Error fetching search results from {args.server}: {e}", file=sys.stderr)
            sys.exit(1)

    saved_path = generate_search_pdf(data, out_file)
    print(f"Successfully generated search PDF report: {os.path.abspath(saved_path)}")


if __name__ == "__main__":
    main()
