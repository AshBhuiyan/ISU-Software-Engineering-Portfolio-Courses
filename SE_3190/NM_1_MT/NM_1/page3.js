// ISU Campus Explorer - Parks Library Script
// Author: Mekhi San
// COM S 3190 - Fall 2025 Midterm Project

fetch("data.json")
  .then(res => res.json())
  .then(data => {
    let container = document.getElementById("buildingContent");
    data.filter(item => item.building === "Parks Library").forEach(entry => {
      let card = document.createElement("div");
      card.classList.add("mb-3");
      card.innerHTML = `
        <img src="${entry.img}" class="img-fluid mb-2" alt="${entry.building}">
        <p>${entry.description}</p>
        <p><strong>Hours:</strong> ${entry.hours}</p>
      `;
      container.appendChild(card);
    });
  });
