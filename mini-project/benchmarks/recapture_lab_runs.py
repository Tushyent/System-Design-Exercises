"""Enhanced re-capture: r11 (telemetry), r12 (ring sim), r13 (throttling), r17 (trie+eval).

Flow:
 1. Fire 50 rapid bad logins in parallel -> auth bucket throttling (401 x10 + 429 x40)
 2. Wait for bucket refill, login as member:
      - 10x URL shorten  -> Snowflake ID generation counted
      - search + detail warm-up -> cache-aside hit counters
 3. Playwright: recapture the 4 System Design Lab screenshots, running the
    ring simulation and the Precision@5 benchmark live in the UI.
"""
import os
import shutil
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor

import requests
from playwright.sync_api import sync_playwright

BASE = "http://localhost:8080"
OUT = r"D:\5th_Semester\SD_Lab\System-Design-Exercises\mini-project\results\screenshots"
APP_URL = "http://localhost:5173"

# ---- 1. Drive the auth rate limiter: 50 rapid bad logins (parallel) --------
def bad_login(_i):
    try:
        r = requests.post(f"{BASE}/api/auth/login",
                          json={"email": "bad@ssn.edu.in", "password": "wrong"},
                          timeout=15)
        return r.status_code
    except Exception as exc:
        return str(exc)


with ThreadPoolExecutor(max_workers=10) as ex:
    codes = list(ex.map(bad_login, range(50)))
statuses = {}
for c in codes:
    statuses[c] = statuses.get(c, 0) + 1
print("auth burst statuses:", statuses)

# ---- 2. Warm caches + snowflake IDs (member session) -----------------------
time.sleep(6)  # let the auth bucket refill (2 tokens/s)
login = requests.post(f"{BASE}/api/auth/login",
                      json={"email": "member@ssn.edu.in", "password": "member123"},
                      timeout=15)
print("member login:", login.status_code)
token = login.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}"}

for i in range(10):
    r = requests.post(f"{BASE}/api/urls/shorten", headers=headers,
                      json={"targetUrl": f"https://ssn.edu.in/library/warmup/{i}",
                            "resourceType": "BOOK",
                            "resourceId": f"warmup-{i}"},
                      timeout=15)
print("url shorten x10 done, last status:", r.status_code)

books = requests.get(f"{BASE}/api/books?page=0&size=12", timeout=15).json()
ids = [b["id"] for b in books["data"]["content"][:5]]
for q in ["data", "clean", "dist"]:
    for _ in range(7):
        requests.get(f"{BASE}/api/books/search?q={q}&mode=optimized&page=0&size=12",
                     timeout=15)
for bid in ids:
    for _ in range(2):
        requests.get(f"{BASE}/api/books/{bid}", timeout=15)
print("cache warm-up done (searches x21, details x10)")

summary = requests.get(f"{BASE}/api/metrics/summary", timeout=15).json()
print("metrics after warm-up:", summary.get("data", summary))


def shot(page, name):
    dest = os.path.join(OUT, name)
    tmp = os.path.join(tempfile.gettempdir(), name)
    page.screenshot(path=tmp)
    last_err = None
    for _ in range(8):
        try:
            shutil.copyfile(tmp, dest)
            last_err = None
            break
        except OSError as exc:
            last_err = exc
            time.sleep(1.5)
    if last_err is not None:
        raise last_err
    try:
        os.remove(tmp)
    except OSError:
        pass
    print(f"  saved {name}")


with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1440, "height": 900}, device_scale_factor=1)

    # ---- r11 telemetry overview (counters: throttling + cache + snowflake) --
    page.goto(APP_URL, wait_until="networkidle")
    page.wait_for_selector("h1")
    page.click("button:has-text('System Design Lab')")
    time.sleep(3.0)
    try:
        page.click("button:has-text('Refresh Telemetry')")
        time.sleep(2.0)
    except Exception:
        pass
    shot(page, "r11_systemdesign_lab_top.png")

    # ---- r12 consistent hashing with simulation results ---------------------
    page.click("button:has-text('Consistent Hashing Ring')")
    time.sleep(1.5)
    page.click("button:has-text('Run Ring Simulation')")
    try:
        page.wait_for_selector("text=compared to", timeout=20000)
    except Exception:
        pass
    time.sleep(1.5)
    shot(page, "r12_systemdesign_hash_ring.png")

    # ---- r13 rate limiter counters (401s + 429s from the burst) ------------
    page.click("button:has-text('Token Bucket Rate Limiter')")
    time.sleep(2.0)
    try:
        page.click("button:has-text('Refresh Telemetry')")
        time.sleep(2.0)
    except Exception:
        pass
    shot(page, "r13_systemdesign_rate_limiter.png")

    # ---- r17 prefix trie: live query + precision benchmark ------------------
    page.click("button:has-text('Prefix Trie Autocomplete')")
    time.sleep(1.5)
    try:
        page.click("button:has-text('Test Query')")
        time.sleep(2.0)
        page.click("button:has-text('Run Precision@5 Benchmark')")
        page.wait_for_selector("text=Benchmark Summary", timeout=20000)
        time.sleep(1.5)
    except Exception:
        pass
    shot(page, "r17_systemdesign_trie.png")

    browser.close()

print("Enhanced re-capture complete.")
