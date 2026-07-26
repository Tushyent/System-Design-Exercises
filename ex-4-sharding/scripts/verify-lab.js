// verify-lab.js
var NUM_STUDENTS = 20;

print("\n=========================================");
print("Task 5: Shard distribution display");
print("=========================================");
var collegeDB = db.getSiblingDB("College");

print("\n--- sh.status() output ---");
sh.status();

print("\n--- getShardDistribution() output ---");
collegeDB.Student.getShardDistribution();

print("\n=========================================");
print("Task 6: Search queries via mongos");
print("=========================================");

var rangeStart = Math.floor(NUM_STUDENTS * 0.1) || 1;
var rangeEnd = Math.floor(NUM_STUDENTS * 0.2) || 2;
print("\n--- Query 1: Find by RollNo Range (" + rangeStart + " to " + rangeEnd + ") ---");
var cursor = collegeDB.Student.find({ RollNo: { $gte: rangeStart, $lte: rangeEnd } });
printjson(cursor.toArray());
print("\nExplain Plan for Query 1:");
printjson(collegeDB.Student.find({ RollNo: { $gte: rangeStart, $lte: rangeEnd } }).explain("executionStats"));

print("\n--- Query 2: Count students in Computer Science department ---");
print("Count: " + collegeDB.Student.countDocuments({ Department: "Computer Science" }));
print("\nExplain Plan for Query 2:");
printjson(collegeDB.Student.find({ Department: "Computer Science" }).explain("executionStats"));

print("\n=========================================");
print("Task 7: Document distribution analysis");
print("=========================================");
var totalDocs = collegeDB.Student.countDocuments();
var splitPoint = Math.floor(NUM_STUDENTS / 2) || 1;
var shard1Docs = collegeDB.Student.countDocuments({ RollNo: { $lt: splitPoint } });
var shard2Docs = collegeDB.Student.countDocuments({ RollNo: { $gte: splitPoint } });

print("Total Documents: " + totalDocs);
print("Documents on Shard 1 (RollNo < " + splitPoint + "): " + shard1Docs);
print("Documents on Shard 2 (RollNo >= " + splitPoint + "): " + shard2Docs);
