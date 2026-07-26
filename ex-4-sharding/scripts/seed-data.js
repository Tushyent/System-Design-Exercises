// seed-data.js
var NUM_STUDENTS = 20;
print("Seeding database with " + NUM_STUDENTS + " student records...");

var collegeDB = db.getSiblingDB("College");
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

collegeDB.Student.insertMany(students);

print("Successfully inserted " + NUM_STUDENTS + " documents into College.Student.");
