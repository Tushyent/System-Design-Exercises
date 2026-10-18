#!/usr/bin/env python3
"""Draw the nine report diagrams with matplotlib (no external tooling needed).

These are used by the "diagram edition" of the report
(Vidya_LMS_MiniProject_Report_Diagrams.docx) where every ChatGPT prompt slot is
replaced by an actual rendered diagram. The prompt edition
(Vidya_LMS_MiniProject_Report.docx) is left untouched.

Output: report/diagrams/d01..d09 *.png  (white background, 150 dpi)
"""

import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
from matplotlib.patches import (Circle, Ellipse, FancyArrowPatch,  # noqa: E402
                                FancyBboxPatch, Polygon, Rectangle)

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "diagrams")

NAVY = "#0C2340"
BLUE = "#1460A3"
SKY = "#DBEAFE"
EMER = "#0F766E"
EMER_L = "#D1FAE5"
GRAY = "#F1F5F9"
GRAY_D = "#475569"
RED = "#DC2626"
RED_L = "#FEE2E2"
AMBER = "#B45309"


def new_ax(w=12, h=6.75):
    fig, ax = plt.subplots(figsize=(w, h), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    return fig, ax


def box(ax, x, y, w, h, text, fc=SKY, ec=BLUE, fs=9.5, tc=NAVY, bold=False, lw=1.4):
    p = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.3,rounding_size=1.4",
                       fc=fc, ec=ec, lw=lw, zorder=2)
    ax.add_patch(p)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs,
            color=tc, fontweight="bold" if bold else "normal", zorder=3)
    return p


def band(ax, x, y, w, h, title):
    p = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.3,rounding_size=1.8",
                       fc=GRAY, ec="#CBD5E1", lw=1.0, zorder=0)
    ax.add_patch(p)
    ax.text(x + 1.2, y + h - 1.8, title, fontsize=8.5, color=GRAY_D,
            fontweight="bold", va="top", zorder=1)
    return p


def arrow(ax, xy1, xy2, color=BLUE, lw=1.6, ls="-", rad=0.0, ms=14):
    a = FancyArrowPatch(xy1, xy2, arrowstyle="-|>", mutation_scale=ms, lw=lw,
                        color=color, linestyle=ls, zorder=4,
                        connectionstyle="arc3,rad=%s" % rad, shrinkA=1, shrinkB=1)
    ax.add_patch(a)
    return a


def label(ax, x, y, text, fs=8, color=GRAY_D, ha="center", bold=False):
    ax.text(x, y, text, fontsize=fs, color=color, ha=ha, va="center",
            fontweight="bold" if bold else "normal", zorder=5)


def cylinder(ax, cx, cy, w, h, top_text, body_text, fc=SKY, ec=BLUE):
    """Stacked-cylinder DB shard glyph; (cx, cy) = centre."""
    x, y = cx - w / 2, cy - h / 2
    ax.add_patch(Rectangle((x, y + 2), w, h - 4, fc=fc, ec="none", zorder=2))
    ax.add_patch(Ellipse((cx, y + 2), w, 4, fc=fc, ec=ec, lw=1.3, zorder=3))
    ax.add_patch(Ellipse((cx, y + h - 2), w, 4, fc=fc, ec=ec, lw=1.3, zorder=3))
    ax.plot([x, x], [y + 2, y + h - 2], color=ec, lw=1.3, zorder=3)
    ax.plot([x + w, x + w], [y + 2, y + h - 2], color=ec, lw=1.3, zorder=3)
    ax.text(cx, cy + h * 0.22, top_text, ha="center", va="center", fontsize=9,
            color=NAVY, fontweight="bold", zorder=4)
    ax.text(cx, cy - h * 0.18, body_text, ha="center", va="center", fontsize=8,
            color=GRAY_D, zorder=4)


# --------------------------------------------------------------------------- #
# d01 - high-level architecture
# --------------------------------------------------------------------------- #
def d01_architecture():
    fig, ax = new_ax(12, 7.2)
    band(ax, 2, 81, 96, 17, "CLIENT TIER")
    box(ax, 8, 83.8, 24, 9, "React SPA browser\n(catalogue, account)")
    box(ax, 38, 83.8, 24, 9, "OPAC kiosk\n(walk-up catalogue)")
    box(ax, 68, 83.8, 24, 9, "Mobile barcode\nscanner client")

    band(ax, 2, 61.5, 96, 16.5, "EDGE / GATEWAY")
    box(ax, 10, 64, 30, 8, "Nginx reverse proxy", fc="white")
    box(ax, 45, 63.6, 45, 9.4,
        "Token-bucket RATE LIMITER (Redis Lua + in-memory fallback)\nauth 10 cap / 2 per s   -   public reads 1500 / 750 per s",
        fc=RED_L, ec=RED, fs=8.5)
    label(ax, 56, 62.6, "HTTP 429 + Retry-After when the bucket is empty", fs=7.5,
          color=RED, bold=True)

    band(ax, 2, 34.5, 96, 21, "SPRING BOOT 3.3 SERVICE TIER  (port 8080)")
    box(ax, 6, 43.9, 27, 7.8, "Auth & Members\n(JWT, Snowflake IDs)")
    box(ax, 36.5, 43.9, 27, 7.8, "Catalogue + Autocomplete\n(prefix trie, search)")
    box(ax, 67, 43.9, 27, 7.8, "Circulation & Reservations\n(atomic findAndModify)")
    box(ax, 6, 35.5, 27, 7.8, "Idempotent Payments\n(receipt replay)", fc=EMER_L, ec=EMER)
    box(ax, 36.5, 35.5, 27, 7.8, "Async Web Crawler\n(TaskExecutor workers)", fc=EMER_L, ec=EMER)
    box(ax, 67, 35.5, 27, 7.8, "Consistent-Hash router\n(160 vnodes, MD5)", fc=EMER_L, ec=EMER)

    band(ax, 2, 5, 96, 25.5, "DATA & INFRASTRUCTURE TIER")
    box(ax, 5, 14.5, 21, 11, "Redis KV cache\ncache-aside\ndetail 10 min / search 60 s TTL", fs=8)
    box(ax, 28, 14.5, 22, 11, "MongoDB :27017\nShard 1 Members (hashed memberId)\nShard 2 Books (category / _id)", fs=7.6)
    box(ax, 52, 14.5, 21, 11, "Snowflake 64-bit\nID generator\nDC 1 - Worker 1", fs=8)
    box(ax, 75, 14.5, 21, 11, "Base62 URL shortener\nGET /s/{code} -> 302\nclick analytics", fs=8)
    label(ax, 50, 9,
          "Borrow = atomic findAndModify   -   Reads = Redis cache-aside   -   "
          "Crawler = publisher feed + ISBN dedup   -   Short link = 302 redirect", fs=8)

    for x in (20, 50, 80):
        arrow(ax, (x, 83.8), (x, 78.3))
    arrow(ax, (25, 64), (25, 55.8))
    arrow(ax, (86, 63.6), (86, 55.8))
    for x in (19.5, 50, 80.5):
        arrow(ax, (x, 35.5), (x, 30.8))

    fig.savefig(os.path.join(OUT, "d01_architecture.png"), bbox_inches="tight",
                facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d02 - document / data model
# --------------------------------------------------------------------------- #
def d02_data_model():
    fig, ax = new_ax(12, 7.5)
    w, h = 30, 16
    xs_l, xs_r = 7, 60
    ys = [76, 54, 32, 10]

    box(ax, xs_l, ys[0], w, h, "members\n_id: Snowflake\nmemberId  (hashed shard key)\nemail (unique) - role\nmaxBorrowLimit", fs=7.6)
    box(ax, xs_l, ys[1], w, h, "loans\nloanId LOAN-...\nmemberId, copyId, bookId\ndueDate - renewalCount (max 2)\nstatus ACTIVE / RETURNED", fs=7.6)
    box(ax, xs_l, ys[2], w, h, "fines\nfineId\nloanId -> loans\noverdueDays\namount = days x Rs.1.50 (cap Rs.50)", fs=7.6)
    box(ax, xs_l, ys[3], w, h, "payments\npaymentId PAY-...\nfineId -> fines\namount - idempotencyKey (unique)\ntransactionRef SIM-TX-...", fs=7.6)

    wr = 33
    box(ax, xs_r, ys[0], wr, h, "books\n_id: Snowflake\nisbn (unique, indexed)\ntitle - authors[] - categories[]\ntotalCopies / availableCopies", fs=7.6)
    box(ax, xs_r, ys[1], wr, h, "book_copies\n_id: Snowflake\nbookId -> books\nbarcode (unique)\nstatus AVAILABLE / BORROWED /\nRESERVED_FOR_PICKUP", fs=7.6)
    box(ax, xs_r, ys[2], wr, h, "reservations\nreservationId RES-...\nbookId -> books\nmemberId -> members\nqueuePosition - status", fs=7.6)
    box(ax, xs_r, ys[3], wr, h, "short_urls\ncode (Base62) -> targetUrl\nresourceType\nclickCount", fs=7.6)

    arrow(ax, (22, ys[0]), (22, ys[1] + h), ms=12)          # members -> loans
    label(ax, 26, (ys[0] + ys[1] + h) / 2, "1 .. *", fs=8)
    arrow(ax, (22, ys[1]), (22, ys[2] + h), ms=12)          # loans -> fines
    label(ax, 26, (ys[1] + ys[2] + h) / 2, "1 .. *", fs=8)
    arrow(ax, (22, ys[2]), (22, ys[3] + h), ms=12)          # fines -> payments
    label(ax, 26, (ys[2] + ys[3] + h) / 2, "1 .. *", fs=8)
    arrow(ax, (76, ys[0]), (76, ys[1] + h), ms=12)          # books -> copies
    label(ax, 80.5, (ys[0] + ys[1] + h) / 2, "1 .. *", fs=8)
    arrow(ax, (37, 62), (60, 60), ms=12)                    # loans -> book_copies
    label(ax, 48.5, 64.3, "copyId", fs=8)
    arrow(ax, (80, ys[1]), (80, ys[2] + h), ms=12)          # copies -> reservations
    label(ax, 84.5, (ys[1] + ys[2] + h) / 2, "wait-list", fs=8)
    ax.plot([88, 96], [48, 48], color=BLUE, lw=1.6, zorder=4)   # from reservations top
    ax.plot([96, 96], [48, 84], color=BLUE, lw=1.6, zorder=4)   # lane right of the boxes
    arrow(ax, (96, 84), (93.4, 84), ms=12)                      # into books right edge
    label(ax, 100, 66, "bookId", fs=8, ha="center")
    arrow(ax, (60, 44), (37, 82), ms=12, rad=-0.25)          # reservations -> members
    label(ax, 44, 57, "memberId", fs=8)

    label(ax, 50, 3.2,
          "Shard keys: members = hashed memberId   -   books = hashed _id / category   -   "
          "loans = compound (memberId, loanId)        |        "
          "Redis: cache:book:{id} (10 min)   -   search:books:{hash} (60 s)", fs=8)
    fig.savefig(os.path.join(OUT, "d02_data_model.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d03 - borrow sequence diagram
# --------------------------------------------------------------------------- #
def d03_sequence():
    fig, ax = new_ax(12, 7)
    lives = [("Member A\n(Thread 1)", 14), ("Member B\n(Thread 2)", 30),
             ("Circulation API", 52), ("MongoDB", 76), ("Redis", 92)]
    for txt, x in lives:
        box(ax, x - 7, 88, 14, 7, txt, fs=8.5, bold=True)
        ax.plot([x, x], [88, 8], color="#94A3B8", lw=1.1, ls="--", zorder=1)

    ax.add_patch(Rectangle((50.6, 18), 2.8, 62, fc=SKY, ec=BLUE, lw=1.0, zorder=2))

    arrow(ax, (14, 78), (52, 78));  label(ax, 33, 80.2, "POST /api/circulation/borrow", fs=7.6)
    arrow(ax, (30, 73), (52, 73));  label(ax, 40, 75.2, "POST /api/circulation/borrow", fs=7.6)
    arrow(ax, (52, 63), (76, 63))
    label(ax, 62.5, 65.4, "findAndModify: copy AVAILABLE -> BORROWED\n(both threads issue the claim in parallel)", fs=7.4)
    arrow(ax, (76, 52), (52, 52), color=EMER)
    label(ax, 64.5, 54.2, "copy claimed (Thread 1)", fs=7.6, color=EMER)
    arrow(ax, (76, 46), (52, 46), color=RED, ls="--")
    label(ax, 64.5, 48.2, "null - copy already taken (Thread 2)", fs=7.6, color=RED)
    arrow(ax, (52, 37), (92, 37), color=AMBER)
    label(ax, 72, 39.2, "DEL cache:book:{id}  (cache eviction)", fs=7.6, color=AMBER)
    arrow(ax, (52, 27), (14, 27), color=EMER)
    label(ax, 32.5, 29.2, "200 OK - loan LOAN-... + due date", fs=7.6, color=EMER)
    arrow(ax, (52, 21), (30, 21), color=RED, ls="--")
    label(ax, 41, 24.2, "409 Conflict - reservation available", fs=7.6, color=RED)

    box(ax, 6, 2, 88, 8,
        "Guarantee: exactly one loan per physical copy - validated by a 20-thread race test\n"
        "(1 success, 19 conflicts) and a 10-thread workflow probe",
        fc="#FFFBEB", ec=AMBER, fs=8.5, tc=AMBER, bold=False)
    ax.text(50, 98.2, "Atomic Borrow - Zero Double-Borrowing", fontsize=12,
            fontweight="bold", color=NAVY, ha="center")
    fig.savefig(os.path.join(OUT, "d03_borrow_sequence.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d04 - token bucket
# --------------------------------------------------------------------------- #
def d04_token_bucket():
    fig, ax = new_ax(12, 6.5)
    label(ax, 14, 92, "Requests", fs=10, bold=True, color=NAVY)
    arrow(ax, (3, 80), (31, 80), color=EMER, ms=13)
    label(ax, 16, 82.6, "HTTP 200 OK - token consumed", fs=8, color=EMER)
    arrow(ax, (3, 62), (31, 62), color=RED, ms=13)
    label(ax, 16, 64.6, "HTTP 429 Too Many Requests + Retry-After", fs=8, color=RED)

    ax.add_patch(Polygon([(34, 78), (60, 78), (56, 46), (38, 46)],
                         closed=True, fc="#EFF6FF", ec=BLUE, lw=1.6, zorder=2))
    ax.plot([33, 61], [78, 78], color=NAVY, lw=2.2, zorder=3)
    tok_xy = [(40, 72), (45, 72), (50, 72), (55, 72),
              (42, 66), (47, 66), (52, 66),
              (43, 60), (48, 60), (53, 60)]
    for tx, ty in tok_xy:
        ax.add_patch(Circle((tx, ty), 1.7, fc="#F59E0B", ec="#B45309", lw=1.0, zorder=4))
    label(ax, 47, 82.7, "capacity C = 10 tokens", fs=9, bold=True, color=NAVY)
    arrow(ax, (47, 93), (47, 85), color=AMBER, ms=13)
    label(ax, 62, 90, "refill r = 2 tokens / s", fs=8.5, color=AMBER, ha="left")
    label(ax, 52, 42.8, "10 tokens held", fs=8, color=GRAY_D, ha="left")
    arrow(ax, (47, 46), (47, 36), ms=13)
    box(ax, 33, 26, 28, 8, "permitted requests\nreach the service", fs=8.5, fc=EMER_L, ec=EMER)

    box(ax, 64, 68, 34, 12, "tokens(t) = min( C, tokens(t-1) + elapsed x r )",
        fs=9.5, fc="white", ec=NAVY)
    box(ax, 64, 46, 34, 10, "Redis atomic Lua EVAL\nkey TTL 60 s", fs=8.5, fc=SKY)
    box(ax, 64, 28, 34, 10, "fallback: in-memory\nConcurrentHashMap bucket", fs=8.5, fc=GRAY)
    arrow(ax, (81, 46), (81, 38), ls="--", color=GRAY_D, ms=12)
    label(ax, 86.5, 42, "failover", fs=8, color=GRAY_D)

    box(ax, 4, 6, 92, 12,
        "Measured burst: 50 concurrent bad logins -> 10 x HTTP 401 permitted + 40 x HTTP 429 throttled\n"
        "= 80% rejection - average limiter overhead 2.58 ms per request",
        fc="#FFFBEB", ec=AMBER, fs=9, tc=AMBER)
    fig.savefig(os.path.join(OUT, "d04_token_bucket.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d05 - sharding
# --------------------------------------------------------------------------- #
def d05_sharding():
    fig, ax = new_ax(12, 6.5)
    box(ax, 3, 38, 22, 16, "Clients\n\nmongos router\n(shard-key aware)", fs=9)
    for cy, top, body in [(68, "Shard A - Members", "hashed memberId"),
                          (40, "Shard B - Books", "hashed _id / category range"),
                          (16, "Shard C - Loans", "compound (memberId, loanId)")]:
        cylinder(ax, 58, cy, 38, 20, top, body)
    arrow(ax, (25, 50), (39, 66), ms=13)
    arrow(ax, (25, 46), (39, 40), ms=13)
    arrow(ax, (25, 42), (39, 18), ms=13)

    box(ax, 80, 52, 18, 32,
        "Uniform load:\nabout 33% of\ndata per shard\n\nHashed keys\navoid write\nhot spots on\nsequential inserts",
        fs=8, fc=EMER_L, ec=EMER)
    box(ax, 80, 12, 18, 32,
        "Category-scoped\nqueries route to\na single shard\n(no scatter-gather)\n\nLoans stay with\ntheir member via\nthe compound key",
        fs=8, fc="white", ec=BLUE)
    label(ax, 50, 2, "GET /api/sharding/stats exposes the declared shard keys per collection", fs=8.5)
    fig.savefig(os.path.join(OUT, "d05_sharding.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d06 - consistent hash ring
# --------------------------------------------------------------------------- #
def d06_ring():
    import math
    fig, ax = new_ax(11.5, 6.8)
    cx, cy, r = 30, 50, 26
    ax.add_patch(Circle((cx, cy), r, fill=False, ec=NAVY, lw=2.0, zorder=2))

    nodes = [("Node-A", 35, BLUE), ("Node-B", 125, BLUE), ("Node-C", 215, BLUE),
             ("Node-D (new)", 305, EMER)]
    # virtual-node ticks: 24 total, coloured by owning quadrant segment
    for k in range(24):
        ang = k * 15
        rad = math.radians(ang)
        seg = int(ang % 360 // 90)
        col = EMER if seg == 3 else BLUE
        x1, y1 = cx + (r - 2) * math.cos(rad), cy + (r - 2) * math.sin(rad)
        x2, y2 = cx + (r + 1.6) * math.cos(rad), cy + (r + 1.6) * math.sin(rad)
        ax.plot([x1, x2], [y1, y2], color=col, lw=2.4, zorder=3)

    for nm, ang, col in nodes:
        rad = math.radians(ang)
        x, y = cx + (r + 7) * math.cos(rad), cy + (r + 7) * math.sin(rad)
        ax.text(x, y, nm, fontsize=9, fontweight="bold", color=col,
                ha="center", va="center")
        ax.add_patch(Circle((cx + (r + 1.6) * math.cos(rad), cy + (r + 1.6) * math.sin(rad)),
                            1.9, fc=col, ec="white", lw=1, zorder=5))

    # keys with clockwise mapping arrows
    for i, ang in enumerate([-15, 52, 100, 148, 196, 250]):
        rad = math.radians(ang)
        kx, ky = cx + (r + 9) * math.cos(rad), cy + (r + 9) * math.sin(rad)
        ax.text(kx, ky, "K%d" % (i + 1), fontsize=8, color=GRAY_D, ha="center", va="center")
        a2 = math.radians(ang + 7)
        arrow(ax, (cx + (r + 6) * math.cos(rad), cy + (r + 6) * math.sin(rad)),
              (cx + (r + 0.5) * math.cos(a2), cy + (r + 0.5) * math.sin(a2)),
              color=GRAY_D, ms=9, lw=1.1)
    ax.text(cx, cy, "2^32 keyspace\n160 vnodes / server\nMD5", fontsize=9, color=NAVY,
            ha="center", va="center", fontweight="bold")

    box(ax, 62, 62, 36, 14,
        "Grow 3 -> 4 nodes\nCONSISTENT HASHING: 28.3% remapped\n(566 of 2,000 keys move)", fs=8.5, fc=EMER_L, ec=EMER)
    box(ax, 62, 44, 36, 14,
        "modulo hash(key) % N:\n75.1% remapped\n(1,502 of 2,000 keys move)", fs=8.5, fc=RED_L, ec=RED)
    box(ax, 62, 26, 36, 12,
        "Only ~1/N keys migrate\n-> warm cache preserved,\nno cache stampede", fs=8.5, fc="#FFFBEB", ec=AMBER)
    ax.text(44, 88, "Keys map clockwise to the next virtual node", fontsize=8.5,
            color=GRAY_D, ha="center")
    fig.savefig(os.path.join(OUT, "d06_hash_ring.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d07 - URL shortener flow
# --------------------------------------------------------------------------- #
def d07_url_shortener():
    fig, ax = new_ax(12, 6)
    label(ax, 4, 86, "Generation path", fs=9.5, bold=True, color=NAVY, ha="left")
    box(ax, 3, 66, 23, 14, "POST /api/urls/shorten\n(long catalogue URL)", fs=8.5)
    box(ax, 30, 66, 20, 14, "Snowflake\n64-bit ID", fs=8.5, fc=EMER_L, ec=EMER)
    box(ax, 54, 66, 19, 14, "Base62 encode\n[0-9a-zA-Z]\ne.g. '23Vp6b'", fs=8.5)
    box(ax, 77, 66, 20, 14, "Store mapping\n+ click counter\nMongoDB / Redis", fs=8.5)
    arrow(ax, (26, 73), (30, 73)); arrow(ax, (50, 73), (54, 73)); arrow(ax, (73, 73), (77, 73))

    label(ax, 4, 52, "Resolution path", fs=9.5, bold=True, color=NAVY, ha="left")
    box(ax, 3, 32, 23, 14, "GET /s/{code}\n(scanned QR / shared link)", fs=8.5)
    box(ax, 30, 32, 20, 14, "O(1) indexed\nlookup", fs=8.5, fc=EMER_L, ec=EMER)
    box(ax, 54, 32, 19, 14, "HTTP 302 Found\nLocation: original URL", fs=8.5, fc=EMER_L, ec=EMER)
    box(ax, 77, 32, 20, 14, "analytics\nclickCount++", fs=8.5)
    arrow(ax, (26, 39), (30, 39)); arrow(ax, (50, 39), (54, 39)); arrow(ax, (73, 39), (77, 39))

    label(ax, 50, 57, "the short code is reused at resolution time", fs=8, color=GRAY_D)

    box(ax, 4, 8, 92, 14,
        "Measured: mean 5.10 ms - P95 6.83 ms per redirection; 50 cached resolutions averaged 1.12 ms.\n"
        "Bijective Base62 over Snowflake IDs: compact, collision-free, QR-kiosk friendly.",
        fc="#FFFBEB", ec=AMBER, fs=8.5, tc=AMBER)
    fig.savefig(os.path.join(OUT, "d07_url_shortener.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d08 - crawler pipeline
# --------------------------------------------------------------------------- #
def d08_crawler():
    fig, ax = new_ax(12, 7)
    box(ax, 3, 54, 20, 20, "Publisher feed\n/mock-external-library/\nbooks\n(7 JSON records)", fs=8)
    box(ax, 28, 54, 22, 20, "AsyncExecutor\nworker pool\n(thread task-1)\n\nfetch -> parse ->\nnormalize", fs=8)
    arrow(ax, (23, 64), (28, 64), ms=13)

    ax.add_patch(Polygon([(52, 62), (63, 73), (74, 62), (63, 51)],
                         closed=True, fc="#FEF3C7", ec=AMBER, lw=1.5, zorder=2))
    ax.text(63, 62, "ISBN already\nin catalogue?", fontsize=7.6, ha="center", va="center",
            color=AMBER, zorder=3, fontweight="bold")
    arrow(ax, (50, 64), (52.2, 62.3), ms=12)

    box(ax, 80, 66, 17, 13, "Enrich record\n(rating, summary)\nDEDUPLICATED", fs=7.6,
        fc=EMER_L, ec=EMER)
    box(ax, 80, 38, 17, 13, "Import title\n+ physical copies\nIMPORTED", fs=7.6)
    label(ax, 71, 72.5, "yes", fs=8.5, color=EMER, bold=True)
    label(ax, 70, 55.5, "no", fs=8.5, color=BLUE, bold=True)
    arrow(ax, (66, 68), (80, 72), ms=12, color=EMER)
    arrow(ax, (66, 56), (80, 46), ms=12)

    box(ax, 50, 12, 40, 14, "MongoDB catalogue +\nCrawlerJob counters\nfetched / parsed / imported /\ndeduplicated / failed", fs=7.8)
    ax.plot([97, 99.3], [72, 72], color=EMER, lw=1.6, zorder=4)
    ax.plot([99.3, 99.3], [72, 19], color=EMER, lw=1.6, zorder=4)
    arrow(ax, (99.3, 19), (90.4, 19), ms=12, color=EMER)
    arrow(ax, (88, 38), (88, 26), ms=12)
    label(ax, 50, 6,
          "Latest run: 7 fetched - 7 deduplicated - 0 imported - 0 failed - 77 ms - 90.9 books/s",
          fs=9, color=AMBER, bold=True)
    fig.savefig(os.path.join(OUT, "d08_crawler_pipeline.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


# --------------------------------------------------------------------------- #
# d09 - autocomplete trie
# --------------------------------------------------------------------------- #
def d09_trie():
    fig, ax = new_ax(12, 6.5)
    cx0, cy0 = 7, 62
    xs = [cx0 + i * 11 for i in range(5)]
    labels = ["root", "d", "i", "s", "t"]
    for i, (x, lab) in enumerate(zip(xs, labels)):
        col = EMER if i >= 1 else NAVY
        ax.add_patch(Circle((x, cy0), 3.2, fc=EMER_L if i >= 1 else SKY, ec=col, lw=1.6, zorder=3))
        ax.text(x, cy0, lab, fontsize=9 if lab != "root" else 7.5, ha="center", va="center",
                color=NAVY, fontweight="bold", zorder=4)
        if i:
            ax.plot([xs[i - 1] + 3.2, x - 3.2], [cy0, cy0], color=EMER, lw=2.2, zorder=2)
    ax.text(xs[0], cy0 - 6, "prefix 'dist' highlighted", fontsize=8, color=GRAY_D, ha="left")

    chips = [("Distributed Systems", "CATEGORY - score 100", 0),
             ("Distributed Systems: Principles\nand Paradigms", "TITLE - score 55", 1),
             ("Distributed Computing: Concepts", "TITLE - score 40", 2)]
    for txt, tag, i in chips:
        y = 74 - i * 13
        box(ax, 60, y, 37, 11, txt, fs=8.2)
        label(ax, 96.5, y + 5.5, "", fs=7)
        ax.text(61.5, y + 1.8, tag, fontsize=7.4, color=EMER, ha="left", va="bottom",
                fontweight="bold", zorder=5)
        arrow(ax, (xs[4] + 3.4, cy0), (60, y + 5.5), rad=-0.12 - i * 0.05, ms=11,
              color=EMER if i == 0 else BLUE)

    box(ax, 3, 30, 32, 10, "search bar:  'dist|'", fs=9, fc="white", ec=NAVY)
    box(ax, 3, 8, 94, 18,
        "Lookup O(L + K log K) - independent of catalogue size  -  in-memory, rebuilt at startup\n"
        "Measured: Precision@5 = 0.46   -   Recall@5 = 0.80   -   average latency 2.32 ms",
        fc="#FFFBEB", ec=AMBER, fs=8.6, tc=AMBER)
    fig.savefig(os.path.join(OUT, "d09_trie.png"), bbox_inches="tight", facecolor="white")
    plt.close(fig)


def main():
    os.makedirs(OUT, exist_ok=True)
    for fn in (d01_architecture, d02_data_model, d03_sequence, d04_token_bucket,
               d05_sharding, d06_ring, d07_url_shortener, d08_crawler, d09_trie):
        fn()
        print("drew", fn.__name__)
    print("diagrams in:", OUT)


if __name__ == "__main__":
    main()
