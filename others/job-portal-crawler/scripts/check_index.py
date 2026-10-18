d = open("src/main/resources/static/index.html", encoding="utf-8").read()
print("len", len(d))
for tag in ["<style>", "</style>", "<script", "</script>", "</body>", "</html>"]:
    print(tag, d.count(tag))
i = d.find("</style>")
print("--- around style close ---")
print(d[i-100:i+200])
idxs = []
j = -1
while True:
    j = d.find("<script", j + 1)
    if j == -1:
        break
    idxs.append(j)
print("script opens at", idxs)
for k in idxs:
    print("...", d[k:k+120].replace("\n", " | "))

