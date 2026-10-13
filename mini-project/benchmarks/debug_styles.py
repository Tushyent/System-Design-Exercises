from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto('http://localhost:5173/')
    page.wait_for_selector('nav')
    
    btn = page.locator('button:has-text("Sign In")')
    print('Button found count:', btn.count())
    if btn.count() > 0:
        b = btn.first
        print('Button tag:', b.evaluate('el => el.tagName'))
        print('Button className:', b.evaluate('el => el.className'))
        print('Button bg:', b.evaluate('el => window.getComputedStyle(el).backgroundColor'))
        print('Button color:', b.evaluate('el => window.getComputedStyle(el).color'))
    
    # Check all buttons on the page
    buttons = page.locator('button').all()
    print(f'Total buttons: {len(buttons)}')
    for i, button in enumerate(buttons[:10]):
        text = button.inner_text().strip().replace('\n', ' ')
        bg = button.evaluate('el => window.getComputedStyle(el).backgroundColor')
        color = button.evaluate('el => window.getComputedStyle(el).color')
        print(f'Btn {i} [{text}]: bg={bg}, color={color}')

    browser.close()
