import json

d = json.load(open("postman/JobPortalCrawler.postman_collection.json"))
print("folders:", [i["name"] for i in d["item"]])
print("total requests:", sum(len(i["item"]) for i in d["item"]))
