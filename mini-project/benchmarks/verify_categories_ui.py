import os
import time
from playwright.sync_api import sync_playwright

SCREENSHOTS_DIR = "results/screenshots"
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

CATEGORIES_TO_TEST = [
    "All",
    "Distributed Systems",
    "System Design",
    "Software Engineering",
    "Computer Science",
    "Electrical Engineering",
    "Mechanical Engineering",
    "Civil Engineering",
    "Mathematics",
    "Data Science",
    "Humanities",
    "General Reading",
]

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={"width": 1440, "height": 950})
    page = context.new_page()

    page.goto("http://localhost:5173/")
    page.wait_for_selector("nav")
    time.sleep(1.5)

    print("=== TESTING ALL CATEGORIES ===")
    for cat in CATEGORIES_TO_TEST:
        # Match button exactly with text
        btn = page.locator(f"button:text-is('{cat}')")
        if btn.count() > 0:
            btn.first.click()
            time.sleep(0.8)
            cards = page.locator("h3").all()
            titles = [c.inner_text().strip() for c in cards if c.inner_text().strip()]
            print(f" -> [{cat}]: {len(titles)} titles -> {titles[:2]}")
        else:
            print(f" -> WARNING: Category '{cat}' button not found!")

    print("\n=== CAPTURING COMPREHENSIVE SCREENSHOTS ===")

    # 1. Reset to All & screenshot Catalogue
    page.locator("button:text-is('All')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/01_catalogue_redesigned.png", full_page=False)
    print("Saved 01_catalogue_redesigned.png")

    # 2. Electrical Engineering Category
    page.locator("button:text-is('Electrical Engineering')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/08_category_electrical.png", full_page=False)
    print("Saved 08_category_electrical.png")

    # 3. Humanities Category
    page.locator("button:text-is('Humanities')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/09_category_humanities.png", full_page=False)
    print("Saved 09_category_humanities.png")

    # 4. Open Book Details Modal on first card
    page.locator("button:text-is('All')").first.click()
    time.sleep(1)
    first_card = page.locator(".group").first
    first_card.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/06_book_detail_modal_redesigned.png")
    print("Saved 06_book_detail_modal_redesigned.png")

    # Close modal
    page.keyboard.press("Escape")
    time.sleep(0.5)

    # 5. Open Sign In modal
    page.locator("button:has-text('Sign In')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/02_auth_modal_redesigned.png")
    print("Saved 02_auth_modal_redesigned.png")

    # Log in as student
    page.locator("button:has-text('Student Member')").first.click()
    time.sleep(1.5)

    # 6. Screenshot My Library as logged-in user
    page.locator("button:has-text('My Library')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/03_my_library_redesigned.png", full_page=False)
    print("Saved 03_my_library_redesigned.png")

    # 7. Screenshot Fines
    page.locator("button:has-text('Fines & Payments')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/04_fines_redesigned.png", full_page=False)
    print("Saved 04_fines_redesigned.png")

    # 8. Screenshot System Design Lab
    page.locator("button:has-text('System Design Lab')").first.click()
    time.sleep(1)
    page.screenshot(path=f"{SCREENSHOTS_DIR}/05_system_design_lab_redesigned.png", full_page=False)
    print("Saved 05_system_design_lab_redesigned.png")

    browser.close()
    print("\nALL VERIFICATIONS AND SCREENSHOTS COMPLETED SUCCESSFULLY!")
