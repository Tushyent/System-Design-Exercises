import pathlib
import re

pat = re.compile(r"job|career|workday|Google|Microsoft|amazon\.jobs|zoho|tcs|netflix", re.I)
skip = {"target", "redis-data", ".git", "gen_postman.py"}
hits = 0
for p in pathlib.Path(".").rglob("*"):
    if not p.is_file() or p.suffix not in {".java", ".py", ".md", ".html", ".yml", ".ps1"}:
        continue
    if any(s in p.parts for s in skip):
        continue
    if p.name == "do_rename.py":
        continue
    for i, line in enumerate(p.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
        if pat.search(line):
            hits += 1
            print(f"{p}:{i}: {line.strip()[:160]}")
print("hits:", hits)
