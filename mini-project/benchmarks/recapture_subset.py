"""Targeted re-capture: r03 (search covers), r11-r13 + r16-r18 (lab tabs), r15 (swagger)."""
import os
import shutil
import tempfile
import time

from playwright.sync_api import sync_playwright

OUT = r"D:\5th_Semester\SD_Lab\System-Design-Exercises\mini-project\results\screenshots"
APP_URL = "http://localhost:5173"
SWAGGER_URL = "http://localhost:8080/swagger-ui.html"


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


def warm_images(page):
    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
    time.sleep(1.5)
    page.evaluate("window.scrollTo(0, 0)")
    try:
        page.wait_for_function(
            "() => Array.from(document.images).every(i => i.complete)", timeout=20000)
    except Exception:
        print("  note: some images still loading")
    time.sleep(1.0)


with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1440, "height": 900}, device_scale_factor=1)

    # ---- r03 search results with covers -----------------------------------
    page.goto(APP_URL, wait_until="networkidle")
    page.wait_for_selector("h1")
    warm_images(page)
    box = page.query_selector("input[aria-label='Search catalogue']")
    box.click()
    box.type("dist", delay=80)
    page.wait_for_selector("text=Autocomplete Matches", timeout=8000)
    box.press("Enter")
    time.sleep(1.0)
    warm_images(page)
    time.sleep(0.5)
    shot(page, "r03_search_results.png")

    # ---- r11-r13, r16-r18 System Design Lab tabs ---------------------------
    page.goto(APP_URL, wait_until="networkidle")
    page.wait_for_selector("h1")
    page.click("button:has-text('System Design Lab')")
    time.sleep(4.0)
    shot(page, "r11_systemdesign_lab_top.png")
    for tab_label, tab_file in [
        ("Consistent Hashing Ring", "r12_systemdesign_hash_ring.png"),
        ("Token Bucket Rate Limiter", "r13_systemdesign_rate_limiter.png"),
        ("Snowflake 64-bit ID", "r16_systemdesign_snowflake.png"),
        ("Prefix Trie Autocomplete", "r17_systemdesign_trie.png"),
        ("Cache Performance Comparator", "r18_systemdesign_caching.png"),
    ]:
        page.click(f"button:has-text('{tab_label}')")
        time.sleep(2.5)
        shot(page, tab_file)

    # ---- r15 swagger with the new Vidya LMS title --------------------------
    page.goto(SWAGGER_URL, wait_until="networkidle")
    time.sleep(3.0)
    shot(page, "r15_swagger_api.png")

    browser.close()

print("Subset re-capture complete.")
