// standalone-comparison.js
print("\n=========================================");
print("Task 8: Sharded vs standalone comparison");
print("=========================================");

var dbStandalone = db.getSiblingDB("CollegeStandalone");

var NUM_STUDENTS = 20;
print("Seeding standalone database with " + NUM_STUDENTS + " records for comparison...");
var students = [];
var departments = ["Computer Science", "Electrical", "Mechanical", "Civil", "Information Tech"];

for (var i = 1; i <= NUM_STUDENTS; i++) {
    students.push({
        RollNo: i,
        Name: "Student_" + i,
        Department: departments[i % departments.length],
        Year: Math.floor(Math.random() * 4) + 1,
        CGPA: parseFloat((Math.random() * 4 + 6).toFixed(2))
    });
}
dbStandalone.Student.insertMany(students);
dbStandalone.Student.createIndex({ RollNo: 1 });
print("Standalone database seeded.");

print("\n--- Running comparison for Range Query ---");
// Timing mongos (sharded)
var connMongos = new Mongo("mongos:27017");
var dbMongos = connMongos.getDB("College");

var startMongos = new Date();
var rangeStart = Math.floor(NUM_STUDENTS * 0.1) || 1;
var rangeEnd = Math.floor(NUM_STUDENTS * 0.9) || 1;
dbMongos.Student.find({ RollNo: { $gte: rangeStart, $lte: rangeEnd } }).toArray();
var endMongos = new Date();
print("Sharded cluster (mongos) time: " + (endMongos - startMongos) + " ms");

// Timing standalone
var startStandalone = new Date();
dbStandalone.Student.find({ RollNo: { $gte: rangeStart, $lte: rangeEnd } }).toArray();
var endStandalone = new Date();
print("Standalone mongod time: " + (endStandalone - startStandalone) + " ms");

print("\n--- Running comparison for Full Collection Scan ---");
var startMongosScan = new Date();
var searchName = "Student_" + (NUM_STUDENTS > 1 ? NUM_STUDENTS - 1 : 1);
dbMongos.Student.find({ Name: searchName }).toArray();
var endMongosScan = new Date();
print("Sharded cluster (mongos) full scan time: " + (endMongosScan - startMongosScan) + " ms");

var startStandaloneScan = new Date();
dbStandalone.Student.find({ Name: searchName }).toArray();
var endStandaloneScan = new Date();
print("Standalone mongod full scan time: " + (endStandaloneScan - startStandaloneScan) + " ms");

print("\nNote: For small datasets like " + NUM_STUDENTS + " records, standalone is often faster due to zero network/routing overhead. Sharding benefits emerge with massive datasets and high concurrency.");
