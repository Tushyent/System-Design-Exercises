import requests
import json

BASE_URL = "http://localhost:8080/api"

r = requests.get(f"{BASE_URL}/books?page=0&size=50")
books = r.json().get("data", {}).get("content", [])

print(f"Total books to audit: {len(books)}\n")
verified_covers = {}

for b in books:
    isbn = b.get("isbn")
    title = b.get("title")
    book_id = b.get("id")
    ol_url = f"https://covers.openlibrary.org/b/isbn/{isbn}-M.jpg"
    
    # Test Open Library
    try:
        resp = requests.get(ol_url, timeout=4)
        # Open Library returns 1x1 gif or small placeholder if missing, or 200 with > 1000 bytes if valid
        if resp.status_code == 200 and len(resp.content) > 1000:
            verified_covers[book_id] = {
                "title": title,
                "isbn": isbn,
                "coverUrl": ol_url,
                "source": "OpenLibrary",
                "bytes": len(resp.content)
            }
            print(f"[FOUND-OL] {title[:40]:40} | ISBN: {isbn} | Size: {len(resp.content)} bytes")
            continue
    except Exception:
        pass
        
    # Test Google Books API for cover image
    try:
        gb_url = f"https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}"
        gb_resp = requests.get(gb_url, timeout=4)
        if gb_resp.status_code == 200:
            items = gb_resp.json().get("items", [])
            if items:
                img_links = items[0].get("volumeInfo", {}).get("imageLinks", {})
                thumbnail = img_links.get("thumbnail") or img_links.get("smallThumbnail")
                if thumbnail:
                    # convert http to https
                    thumbnail = thumbnail.replace("http://", "https://")
                    verified_covers[book_id] = {
                        "title": title,
                        "isbn": isbn,
                        "coverUrl": thumbnail,
                        "source": "GoogleBooks",
                        "bytes": -1
                    }
                    print(f"[FOUND-GB] {title[:40]:40} | ISBN: {isbn} | GB Thumbnail")
                    continue
    except Exception:
        pass

    print(f"[FALLBACK] {title[:40]:40} | ISBN: {isbn} | Using Procedural Theme")

print(f"\nAudit Complete: {len(verified_covers)} / {len(books)} books matched real cover imagery!")
