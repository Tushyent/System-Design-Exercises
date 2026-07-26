// init-router.js
var NUM_STUDENTS = 20;

print("\n=========================================");
print("Task 2: Shards added to cluster");
print("=========================================");
printjson(db.adminCommand({ addShard: "shard1RS/shard1:27018" }));
printjson(db.adminCommand({ addShard: "shard2RS/shard2:27028" }));

print("\n=========================================");
print("Task 3: Sharding enabled for College DB");
print("=========================================");
printjson(db.adminCommand({ enableSharding: "College" }));

print("\n=========================================");
print("Task 4: Student collection created and sharded");
print("=========================================");
var collegeDB = db.getSiblingDB("College");
collegeDB.Student.createIndex({ RollNo: 1 });
print("Index created on { RollNo: 1 }.");
printjson(db.adminCommand({ shardCollection: "College.Student", key: { RollNo: 1 } }));

var splitPoint = Math.floor(NUM_STUDENTS / 2) || 1;
print("\nPre-splitting chunks at RollNo = " + splitPoint + " to distribute data...");
printjson(db.adminCommand({ split: "College.Student", middle: { RollNo: splitPoint } }));

print("Moving upper chunk [" + splitPoint + ", MaxKey] to shard2RS...");
sleep(2000);
var findDoc = splitPoint + 1;
printjson(db.adminCommand({ moveChunk: "College.Student", find: { RollNo: findDoc }, to: "shard2RS" }));

print("\nRouter setup complete!");
