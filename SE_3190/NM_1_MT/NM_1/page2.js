// ISU Campus Explorer - Campus Map Script
// Author: Ash Bhuiyan
// COM S 3190 - Fall 2025 Midterm Project

fetch("data.json")
    .then(res => res.json())
    .then(data => {
        let container = document.getElementById("mapContent");
        data.forEach(item => {
            let card = document.createElement("div");
            card.classList.add("card", "mb-3");
            card.innerHTML = `
<img src="${item.img}" class="card-img-top" alt="${item.building}">
<div class="card-body">
    <h5 class="card-title">${item.building}</h5>
    <p>${item.description}</p>
    <a href="${item.building.includes('Parks') ? 'page3.html' : 'page4.html'}" class="btn btn-primary">View Details</a>
</div>
`;
            container.appendChild(card);
        });
    });
