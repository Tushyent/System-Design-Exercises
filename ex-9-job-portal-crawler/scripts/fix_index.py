d = open("src/main/resources/static/index.html", encoding="utf-8").read()
# Keep everything up to the end of the SECOND </script> (real page end first pass)
idx = -1
for _ in range(2):
    idx = d.index("</script>", idx + 1)
cut = idx + len("</script>")
# then append proper closing tags
fixed = d[:cut] + "\n</body>\n</html>\n"
open("src/main/resources/static/index.html", "w", encoding="utf-8").write(fixed)
print("truncated to", len(fixed))
