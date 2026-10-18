import shutil

shutil.copy(
    "src/main/resources/static/index.html",
    "src/main/resources/static/index.html.bak",
)
print("backup done")
