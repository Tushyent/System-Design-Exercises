import os
import time
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:5173')
    page.wait_for_selector('h1')

    # 1. Click on the first book card to open BookDetailModal
    first_book = page.query_selector("h3:has-text('Strict Concurrency Verification Book')")
    if not first_book:
        first_book = page.query_selector('.cursor-pointer')
    if first_book:
        first_book.click()
        time.sleep(1)
        page.screenshot(path='results/screenshots/06_book_detail_modal.png')
        print("Saved 06_book_detail_modal.png")

        # Close modal
        close_btn = page.query_selector("button[aria-label='Close dialog']")
        if close_btn:
            close_btn.click()
            time.sleep(0.5)

    # 2. Sign in as Admin
    sign_in_btn = page.query_selector("button:has-text('Sign In')")
    if not sign_in_btn:
        # If logged in, logout first
        logout_btn = page.query_selector("button[aria-label='Sign Out']")
        if logout_btn:
            logout_btn.click()
            time.sleep(0.5)
            sign_in_btn = page.query_selector("button:has-text('Sign In')")

    if sign_in_btn:
        sign_in_btn.click()
        page.wait_for_selector('#auth-modal-title')
        admin_quick = page.query_selector("button:has-text('System Admin')")
        if admin_quick:
            admin_quick.click()
            time.sleep(1)

            # Navigate to Admin desk
            admin_tab = page.query_selector("button:has-text('Library Operations')")
            if admin_tab:
                admin_tab.click()
                time.sleep(0.5)
                page.screenshot(path='results/screenshots/07_admin_operations.png')
                print("Saved 07_admin_operations.png")

    browser.close()
print("Additional modal & admin screenshots captured!")
