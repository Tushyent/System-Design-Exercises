import json
import urllib.request

BASE = "http://localhost:8081"


def call(method, path):
    req = urllib.request.Request(BASE + path, method=method)
    with urllib.request.urlopen(req, timeout=10) as r:
        print(method, path, "->", r.status, r.read().decode()[:120])


call("POST", "/api/reset")
for c in ("demo-client", "burst-client", "fresh-client"):
    call("DELETE", "/api/bucket/" + c)
print("clean state ready for Postman demo")
