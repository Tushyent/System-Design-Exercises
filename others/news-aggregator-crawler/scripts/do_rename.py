"""Bulk-retarget the ex-9 copy to the news aggregator. Run from INSIDE ex-10."""
import pathlib

ROOT = pathlib.Path(".")

RULES = [
    ("com.jobportal", "com.newsagg"),
    ("JobPortalCrawler", "NewsAggregatorCrawler"),
    ("JobPortalApplication", "NewsAggregatorApplication"),
    ("JobPortal", "NewsAggregator"),
    ("JobCrawler", "NewsCrawler"),
    ("MockJobSite", "MockNewsSite"),
    ("Job Portal", "News Aggregator"),
    ("job-portal-crawler", "news-aggregator-crawler"),
    ("jobportal-net", "newsagg-net"),
    ("jobportal", "newsagg"),
    ("jobcrawl", "newscrawl"),
    ("JobLinks", "ArticleLinks"),
    ("jobLinks", "articleLinks"),
    ("JobLink", "ArticleLink"),
    ("job-listing", "news"),
    ("Job-listing", "News"),
    ("job posting", "news article"),
    ("Job posting", "News article"),
    ("job-posting", "news-article"),
    ("job hyperlinks", "article hyperlinks"),
    ("new job links", "new article links"),
    ("job links", "article links"),
    ("Job links", "Article links"),
    ("job link", "article link"),
    ("job-ish", "article-ish"),
    ("Job Discovery", "News Discovery"),
    ("job discovery", "news discovery"),
    ("Mock Jobs", "Mock News"),
    ("mock/jobs", "mock/news"),
    ("class CrawlerServiceTest", "class NewsCrawlerServiceTest"),
    ('"6379:6379"', '"6380:6379"'),
    ('"8080:8080"', '"8081:8080"'),
    ("localhost:8080", "localhost:8081"),
    ("server.port=8080", "server.port=8081"),
    ("REDIS_PORT:6379", "REDIS_PORT:6380"),
]

SKIP_DIRS = {"target", "redis-data", ".git"}
SKIP_FILES = {"do_rename.py"}

count = 0
for p in ROOT.rglob("*"):
    if not p.is_file():
        continue
    if any(d in p.parts for d in SKIP_DIRS):
        continue
    if p.name in SKIP_FILES or p.suffix == ".json":
        continue
    try:
        text = p.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        continue
    new = text
    for old, new_ in RULES:
        new = new.replace(old, new_)
    if new != text:
        p.write_text(new, encoding="utf-8")
        count += 1
        print("updated", p)
print("files updated:", count)
