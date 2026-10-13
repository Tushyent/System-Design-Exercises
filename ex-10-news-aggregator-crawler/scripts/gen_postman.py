"""Generate the news Postman collection JSON."""
import json

BASE = "http://localhost:8081"
SEED = BASE + "/mock/news"


def crawl(name, client, seed=SEED):
    return {
        "name": name,
        "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/crawl",
            "header": [
                {"key": "Content-Type", "value": "application/json"},
                {"key": "X-Client-Id", "value": client},
            ],
            "body": {"mode": "raw", "raw": '{\n  "seedUrl": "%s"\n}' % seed},
        },
    }


def get(name, url):
    return {"name": name, "request": {"method": "GET", "url": url}}


collection = {
    "info": {
        "name": "News Aggregator Crawler - Postman Demo",
        "description": "Q(a) news crawl demo, Q(b) burst/429 demo, Q(c) analysis. Run folders in order.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8081"},
        {"key": "clientId", "value": "demo-client"},
    ],
    "item": [
        {"name": "0. Seeds", "item": [
            get("Mock news front page (browser)", "{{baseUrl}}/mock/news"),
            get("Seed URLs (API)", "{{baseUrl}}/api/mock-urls"),
        ]},
        {"name": "1. (a) Normal crawl", "item": [
            crawl("POST /api/crawl - front page seed", "{{clientId}}"),
            get("GET /api/queue", "{{baseUrl}}/api/queue?limit=50"),
            get("GET /api/visited", "{{baseUrl}}/api/visited"),
            get("GET /api/bucket/demo-client", "{{baseUrl}}/api/bucket/{{clientId}}"),
        ]},
        {"name": "2. (a) Duplicate demo", "item": [
            crawl("POST /api/crawl - same seed again", "{{clientId}}"),
        ]},
        {"name": "3. (b) Burst 8x (5x200 then 429)", "item": (
            [{"name": "Reset bucket first",
              "request": {"method": "DELETE", "url": "{{baseUrl}}/api/bucket/burst-client"}}]
            + [crawl("Burst #%d" % i, "burst-client") for i in range(1, 7)]
            + [crawl("Burst #7 - expect 429", "burst-client")]
            + [crawl("Burst #8 - expect 429 + Retry-After", "burst-client")]
        )},
        {"name": "4. (b) Refill + isolation", "item": [
            crawl("Retry after 6s wait - 200 again", "burst-client",
                  "http://localhost:8081/mock/news/section/technology"),
            crawl("Fresh client - 200 (isolated bucket)", "fresh-client"),
            crawl("Real news site (needs internet)", "real-client",
                  "https://news.ycombinator.com/"),
        ]},
        {"name": "5. Reset", "item": [
            {"name": "POST /api/reset",
             "request": {"method": "POST", "url": "{{baseUrl}}/api/reset"}},
            {"name": "DELETE /api/bucket/demo-client",
             "request": {"method": "DELETE", "url": "{{baseUrl}}/api/bucket/demo-client"}},
        ]},
    ],
}

with open("postman/NewsAggregatorCrawler.postman_collection.json", "w") as f:
    json.dump(collection, f, indent=2)
print("collection written")
