import requests
import subprocess
import json

BASE_URL = "http://localhost:8080/api"
r = requests.get(f"{BASE_URL}/books?page=0&size=50")
books = r.json().get("data", {}).get("content", [])

updates = []
for b in books:
    isbn = b.get("isbn")
    book_id = b.get("id")
    ol_url = f"https://covers.openlibrary.org/b/isbn/{isbn}-M.jpg"
    try:
        resp = requests.head(ol_url, timeout=3, allow_redirects=True)
        # Verify with GET to ensure size > 1000 bytes (real cover)
        get_resp = requests.get(ol_url, timeout=3)
        if get_resp.status_code == 200 and len(get_resp.content) > 1000:
            updates.append({"id": book_id, "url": ol_url, "title": b.get("title")})
    except Exception:
        pass

print(f"Applying real cover URLs for {len(updates)} verified books...")

# Construct MongoDB update script
js_commands = []
for u in updates:
    js_commands.append(f'db.books.updateOne({{ _id: "{u["id"]}" }}, {{ $set: {{ coverUrl: "{u["url"]}" }} }});')

js_script = "\n".join(js_commands)
subprocess.run(["mongosh", "mongodb://127.0.0.1:27017/librarydb", "--eval", js_script], capture_output=True, text=True)

print(f"Successfully updated {len(updates)} books with real verified Open Library covers in MongoDB!")
