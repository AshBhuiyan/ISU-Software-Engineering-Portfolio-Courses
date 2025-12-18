// Author: Ash Bhuiyan
// Email: mbhuiyan@iastate.edu
// Date: 10/12/2025

// Fetch JSON file
fetch("./movies.json")
  .then(response => response.json())
  .then(myMovies => loadMovies(myMovies))
  .catch(error => console.error("Error loading JSON:", error));

// Load Movies Dynamically
function loadMovies(myMovies) {
  var CardMovie = document.getElementById("col");
  var checkboxes = [];
  var cards = [];

  // Loop through movies
  for (var i = 0; i < myMovies.movies.length; i++) {
    let title = myMovies.movies[i].title;
    let year = myMovies.movies[i].year;
    let url = myMovies.movies[i].url;

    // Column container
    let AddCardMovie = document.createElement("div");
    AddCardMovie.classList.add("col");

    // Unique IDs
    let checkbox = "checkbox" + i.toString();
    let card = "card" + i.toString();

    // Insert HTML and append
    AddCardMovie.innerHTML = `
      <input type="checkbox" id=${checkbox} class="form-check-input" checked>
      <label for=${checkbox} class="form-check-label">Show Image ${i + 1}</label>

      <div id=${card} class="card shadow-sm mt-2">
        <img src=${url} class="card-img-top" alt="${title}">
        <div class="card-body">
          <p class="card-text">
            <strong>${title}</strong><br>${year}
          </p>
        </div>
      </div>
    `;

    CardMovie.appendChild(AddCardMovie);

    // Toggle logic
    let cbox = document.getElementById(checkbox);
    let ccard = document.getElementById(card);

    checkboxes.push(cbox);
    cards.push(ccard);
  }

  checkboxes.forEach((checkboxParam, index) => {
    checkboxParam.addEventListener("change", () => {
      cards[index].style.display = checkboxParam.checked ? "block" : "none";
    });
  });
}
