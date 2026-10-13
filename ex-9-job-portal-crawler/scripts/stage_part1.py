"""Part 1: CSS + BODY of the dashboard."""

CSS = open("scripts/index_css.html").read()
BODY = open("scripts/index_body.html").read()

open("scripts/index_part1.txt", "w").write(CSS + "\n@@@SPLIT@@@\n" + BODY)
print("part1 staged")
