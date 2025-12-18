// JavaScript code for the project corresponding to authors.html. Do not rename this file.
// ISU Campus Explorer - Authors Page Script
// Author: Mekhi San
// COM S 3190 - Fall 2025 Midterm Project

const authors = [
  {
    name: "Mekhi San",
    email: "sanm20@iastate.edu",
    bio: "Frontend developer, UI/UX design focus.",
    img: "assets/images/mekhi.jpg"
  },
  {
    name: "Ash Bhuiyan",
    email: "mbhuiyan@iastate.edu",
    bio: "Backend integration, data handling.",
    img: "assets/images/ash.jpeg"
  }
];

let container = document.getElementById("authorsContent");
authors.forEach(a => {
  let col = document.createElement("div");
  col.classList.add("col-md-6", "mb-4");
  col.innerHTML = `
    <div class="card h-100">
      <img src="${a.img}" class="card-img-top" alt="${a.name}">
      <div class="card-body">
        <h5 class="card-title">${a.name}</h5>
        <p class="card-text"><strong>Email:</strong> ${a.email}</p>
        <p class="card-text">${a.bio}</p>
      </div>
    </div>
  `;
  container.appendChild(col);
});
