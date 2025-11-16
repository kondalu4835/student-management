let students = JSON.parse(localStorage.getItem("students")) || [];

const nameInput = document.getElementById("name");
const emailInput = document.getElementById("email");
const ageInput = document.getElementById("age");
const courseInput = document.getElementById("course");
const marksInput = document.getElementById("marks");

const addBtn = document.getElementById("addBtn");
const sortBtn = document.getElementById("sortBtn");
const searchInput = document.getElementById("search");
const tableBody = document.getElementById("tableBody");


function validateInput(name, email, age, course, marks) {
    if (!name || !email || !age || !course || marks === "") {
        alert("All fields are required!");
        return false;
    }

    if (!email.includes("@") || !email.includes(".")) {
        alert("Enter a valid email address!");
        return false;
    }

    if (age < 18 || age > 40) {
        alert("Age must be between 18 and 40!");
        return false;
    }

    if (marks < 0 || marks > 100) {
        alert("Marks must be between 0 and 100!");
        return false;
    }

    return true;
}


addBtn.addEventListener("click", () => {
    const name = nameInput.value.trim();
    const email = emailInput.value.trim();
    const age = Number(ageInput.value);
    const course = courseInput.value;
    const marks = Number(marksInput.value);

    if (!validateInput(name, email, age, course, marks)) return;

    const student = { name, email, age, course, marks };
    students.push(student);

    localStorage.setItem("students", JSON.stringify(students));

    displayStudents(students);

    nameInput.value = "";
    emailInput.value = "";
    ageInput.value = "";
    courseInput.value = "";
    marksInput.value = "";

    alert("Student added successfully!");
});


function displayStudents(list) {
    tableBody.innerHTML = "";

    list.forEach((student) => {
        const tr = document.createElement("tr");

        
        let markClass = "";
        if (student.marks > 80) markClass = "green";
        else if (student.marks >= 50) markClass = "orange";
        else markClass = "red";

        tr.innerHTML = `
            <td>${student.name}</td>
            <td>${student.email}</td>
            <td>${student.age}</td>
            <td>${student.course}</td>
            <td class="${markClass}">${student.marks}</td>
        `;

        tableBody.appendChild(tr);
    });
}

displayStudents(students);

searchInput.addEventListener("keyup", () => {
    const keyword = searchInput.value.toLowerCase();

    const filtered = students.filter(s =>
        s.name.toLowerCase().includes(keyword)
    );

    displayStudents(filtered);
});

sortBtn.addEventListener("click", () => {
    students.sort((a, b) => b.marks - a.marks);

    localStorage.setItem("students", JSON.stringify(students));

    displayStudents(students);
});
