"""
Report Screenshot Capture (Playwright)
======================================
Captures the full evidence set for the mini-project Word report from the
live system (backend :8080 + frontend :5173 + Redis + MongoDB).

Output: results/screenshots/r*.png
"""
import os
import shutil
import tempfile
import time

from playwright.sync_api import sync_playwright

OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "results", "screenshots"))
os.makedirs(OUT, exist_ok=True)

APP_URL = "http://localhost:5173"
SWAGGER_URL = "http://localhost:8080/swagger-ui.html"


def shot(page, name):
    """Screenshot with temp-file staging + retry (avoids transient AV file locks)."""
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
    """Scroll through the page to trigger lazy image loads, then return to top."""
    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
    time.sleep(1.5)
    page.evaluate("window.scrollTo(0, 0)")
    try:
        page.wait_for_function(
            "() => Array.from(document.images).every(i => i.complete)", timeout=20000)
    except Exception:
        print("  note: some images still loading")
    time.sleep(1.0)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900}, device_scale_factor=1)

        # -------- 1. Public catalogue (guest) ------------------------------
        page.goto(APP_URL, wait_until="networkidle")
        page.wait_for_selector("h1")
        warm_images(page)
        time.sleep(0.5)
        shot(page, "r01_catalogue_guest.png")

        # -------- 2. Autocomplete dropdown with latency badge --------------
        box = page.query_selector("input[aria-label='Search catalogue']")
        box.click()
        box.type("dist", delay=80)
        page.wait_for_selector("text=Autocomplete Matches", timeout=8000)
        time.sleep(0.5)
        shot(page, "r02_autocomplete.png")

        # -------- 3. Search results grid -----------------------------------
        box.press("Enter")
        time.sleep(1.0)
        warm_images(page)
        time.sleep(0.5)
        shot(page, "r03_search_results.png")

        clear_btn = page.query_selector("button[title='Clear search']")
        if clear_btn:
            clear_btn.click()
            time.sleep(1.0)

        # -------- 4. Authentication modal ----------------------------------
        page.click("button:has-text('Sign In')")
        page.wait_for_selector("#auth-modal-title")
        time.sleep(0.5)
        shot(page, "r04_auth_modal.png")

        # -------- 5. Catalogue as logged-in member -------------------------
        page.click("button:has-text('Student Member')")
        page.wait_for_selector("button[aria-label='Sign Out']", timeout=10000)
        time.sleep(1.5)
        shot(page, "r05_catalogue_member.png")

        # -------- 6. Book detail modal (member view) -----------------------
        card = page.locator("div[role='button']:has(h3)").first
        if card:
            card.click()
            page.wait_for_selector("button[aria-label='Close dialog']", timeout=8000)
            try:
                page.wait_for_function(
                    "() => Array.from(document.images).every(i => i.complete)", timeout=15000)
            except Exception:
                pass
            time.sleep(1.2)
            shot(page, "r06_book_detail_modal.png")
            page.click("button[aria-label='Close dialog']")
            time.sleep(0.8)

        # -------- 7. My Library (loans, renewals, reservations) ------------
        page.click("button:has-text('My Library')")
        time.sleep(1.5)
        shot(page, "r07_my_library.png")

        # -------- 8. Fines & Payments --------------------------------------
        page.click("button:has-text('Fines & Payments')")
        time.sleep(1.5)
        shot(page, "r08_fines_unpaid.png")

        # -------- 9. Fine settlement flow ----------------------------------
        pay_btn = page.query_selector("button:has-text('Pay Fine')")
        if pay_btn:
            pay_btn.click()
            time.sleep(0.8)
            shot(page, "r09_payment_modal.png")
            confirm = page.query_selector("button:has-text('Confirm Simulated Payment')")
            if confirm:
                confirm.click()
                time.sleep(2.0)
                shot(page, "r10_payment_receipt.png")
        else:
            print("  note: no unpaid fine visible; skipping settlement capture")

        # -------- 10. System Design Lab dashboard --------------------------
        page.click("button:has-text('System Design Lab')")
        time.sleep(4.0)  # allow live polling + charts to settle
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

        # -------- 11. Admin operations desk --------------------------------
        page.click("button[aria-label='Sign Out']")
        time.sleep(1.0)
        page.click("button:has-text('Sign In')")
        page.wait_for_selector("#auth-modal-title")
        page.click("button:has-text('System Admin')")
        page.wait_for_selector("button[aria-label='Sign Out']", timeout=10000)
        time.sleep(1.2)
        admin_tab = page.query_selector("button:has-text('Library Operations')")
        if admin_tab:
            admin_tab.click()
            time.sleep(1.5)
        shot(page, "r14_admin_operations.png")

        # -------- 12. Swagger / OpenAPI docs -------------------------------
        page.goto(SWAGGER_URL, wait_until="networkidle")
        time.sleep(3.0)
        shot(page, "r15_swagger_api.png")

        browser.close()
        print("All report screenshots captured.")


if __name__ == "__main__":
    main()
