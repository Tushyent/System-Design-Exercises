import os
import time
from playwright.sync_api import sync_playwright

os.makedirs('results/screenshots', exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:5173')
    page.wait_for_selector('h1')

    # 1. Check Sign in button
    btn = page.query_selector("button:has-text('Sign In')")
    print("Sign In button found:", btn is not None)
    if btn:
        print("Sign in bounding box:", btn.bounding_box())

    # Take Catalogue screenshot
    page.screenshot(path='results/screenshots/01_catalogue.png')
    print("Saved 01_catalogue.png")

    # 2. Open Auth Modal
    if btn:
        btn.click()
        page.wait_for_selector('#auth-modal-title')
        page.screenshot(path='results/screenshots/02_auth_modal.png')
        print("Saved 02_auth_modal.png")

        # Click Student Member quick-fill
        student_btn = page.query_selector("button:has-text('Student Member')")
        if student_btn:
            student_btn.click()
            time.sleep(1)
            print("Logged in as student member!")

    # 3. Screenshot My Library (Loans)
    loans_tab = page.query_selector("button:has-text('My Library')")
    if loans_tab:
        loans_tab.click()
        time.sleep(0.5)
        page.screenshot(path='results/screenshots/03_my_library.png')
        print("Saved 03_my_library.png")

    # 4. Screenshot Fines
    fines_tab = page.query_selector("button:has-text('Fines & Payments')")
    if fines_tab:
        fines_tab.click()
        time.sleep(0.5)
        page.screenshot(path='results/screenshots/04_fines.png')
        print("Saved 04_fines.png")

    # 5. Screenshot System Design Lab
    sd_tab = page.query_selector("button:has-text('System Design Lab')")
    if sd_tab:
        sd_tab.click()
        time.sleep(0.5)
        page.screenshot(path='results/screenshots/05_system_design_lab.png')
        print("Saved 05_system_design_lab.png")

    browser.close()
print("All screenshots captured!")
