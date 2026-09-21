import os
import time
from playwright.sync_api import sync_playwright

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "assets", "images")
os.makedirs(ASSETS, exist_ok=True)

def capture():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 900})
        
        # 1. Main Dashboard
        print("Navigating to http://localhost:8080/ ...")
        page.goto("http://localhost:8080/")
        page.wait_for_load_state("networkidle")
        time.sleep(2)
        
        # Trigger single crawl on UI
        page.click("button:has-text('Fetch URL')")
        time.sleep(1)
        
        dash_path = os.path.join(ASSETS, "crawler_dashboard.png")
        page.screenshot(path=dash_path, full_page=False)
        print(f"Captured: {dash_path}")
        
        # 2. Table view
        table_path = os.path.join(ASSETS, "crawled_pages_table.png")
        table_elem = page.locator(".table-card")
        if table_elem.count() > 0:
            table_elem.screenshot(path=table_path)
            print(f"Captured: {table_path}")
            
        # 3. Single Crawl Result view
        single_elem = page.locator("#single-result-box")
        if single_elem.count() > 0:
            single_path = os.path.join(ASSETS, "single_crawl_result.png")
            single_elem.screenshot(path=single_path)
            print(f"Captured: {single_path}")

        # 4. Swagger UI
        print("Navigating to Swagger UI...")
        page.goto("http://localhost:8080/swagger-ui/index.html")
        page.wait_for_load_state("networkidle")
        time.sleep(2)
        swagger_path = os.path.join(ASSETS, "swagger_ui.png")
        page.screenshot(path=swagger_path, full_page=False)
        print(f"Captured: {swagger_path}")

        browser.close()

if __name__ == "__main__":
    capture()
