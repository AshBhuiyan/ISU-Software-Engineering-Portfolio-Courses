// JavaScript code for the project corresponding to faq.html. Do not rename this file.
// ISU Campus Explorer - FAQ Page Script
// Author: Ash Bhuiyan
// COM S 3190 - Fall 2025 Midterm Project

// FAQ data
const faqs = [
    {
        question: "What is ISU Campus Explorer?",
        answer: "ISU Campus Explorer is a student-built web app that helps users explore Iowa State University's campus buildings, including Parks Library and Carver Hall."
    },
    {
        question: "What technologies are used?",
        answer: "The project uses HTML5, Bootstrap 5, and JavaScript for interactivity, with a unified styling file (assets/theme.css) shared across all pages."
    },
    {
        question: "Who created this project?",
        answer: "Team NM_1 — Mekhi San and Ash Bhuiyan. Mekhi focused on the landing and Parks Library pages; Ash developed the Campus Map, Carver Hall and FAQ pages."
    },
    {
        question: "What data does the site use?",
        answer: "Building details and images are stored locally in data.json, which each page’s JavaScript file dynamically loads and displays."
    },
    {
        question: "Will more buildings be added?",
        answer: "Yes! Future versions may include more ISU landmarks such as Hoover Hall, Memorial Union, and the Student Innovation Center."
    },
    {
        question: "Can users contribute?",
        answer: "Students can suggest improvements and feature ideas via the team’s GitLab repository."
    }
];

// Select container
const container = document.getElementById("faqAccordion");

// Populate accordion dynamically
faqs.forEach((faq, index) => {
    const item = document.createElement("div");
    item.classList.add("accordion-item");
    item.innerHTML = `
    <h2 class="accordion-header" id="heading${index}">
      <button class="accordion-button collapsed" type="button"
              data-bs-toggle="collapse" data-bs-target="#collapse${index}">
        ${faq.question}
      </button>
    </h2>
    <div id="collapse${index}" class="accordion-collapse collapse"
         data-bs-parent="#faqAccordion">
      <div class="accordion-body">${faq.answer}</div>
    </div>
  `;
    container.appendChild(item);
});