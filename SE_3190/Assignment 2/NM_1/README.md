# SkillFlix

## Project Overview

A Netflix‑style course browsing and enrollment app built with React, React Router, and Tailwind CSS. SkillFlix lets users discover courses, view details, enroll with a short form, and complete a simulated payment & confirmation flow. Inspired by Netflix‑style platforms for fast, familiar navigation.

## Purpose

Practice multi‑view React apps using Router and Hooks. Render dynamic JSON data, pass state/props between views, and apply Tailwind for a responsive UI. Reinforce teamwork, readable structure, and basic form validation.

## Features

• Home (Browse Courses): Horizontal rows of course cards with quick previews.  
• Course Details: Title, description, duration, level, rating, price, and syllabus; “Enroll” CTA.  
• Enrollment Form: Name, email, plan/schedule; client‑side validation.  
• Payment & Confirmation: Simulated payment step and friendly receipt page.

## Setup Instructions

- How to clone the repo.
- How to install dependencies.
- How to run the app (`npm install` / `npm run dev`).

## Team Members & Roles

- Member 1: Responsibilities
  - Mekhi San
  - Course details
  - Enrollment Form
  - Authors
  - Main.jsx
  - Courses.json
  - Categoryrole
  - package.json
  - Tailwind

- Member 2: Responsibilities
  - App.jsx
  - Components:
  - Home
  - Navbar
  - Paymentconfir
  - Faq
  - index.jsx
  - Course card
  - Postcssconfig
  - Viteconfig

## Design Summary

Our goal with SkillFlix was to make it feel as close to Netflix as possible while still behaving like a focused course platform, so we went with a dark, cinematic layout: a sticky header with the SKILLFLIX logo and simple navigation, a full-bleed hero banner that spotlights a “Featured Course,” and horizontal rows of courses by category so you can browse “Software Development,” “AI & Machine Learning,” and “Theory of Computation” the same way you’d scroll through shows. Each course tile grows, lifts, and updates the hero on hover, which keeps the page feeling interactive and helps you quickly preview different courses without extra clicks. Tailwind powers almost everything here—spacing, typography, gradients, and hover states—so using flexbox and grid utilities we made sure the layout adapts cleanly from laptop to phone: cards resize, rows scroll horizontally instead of breaking awkwardly, and text stays readable against the dark background. The same visual language continues into the Course Details, Enrollment, Payment, FAQ, and Authors pages, where we use simple card-style sections, clear hierarchy, and just enough color to highlight actions like “Enroll Now” without overwhelming the content. Overall, the design aims to feel familiar and binge-able like a streaming app, but underneath it stays practical for students: easy to scan, smooth to navigate, and consistent across every step of the learning flow.

## Demo

- Video Link: https://drive.google.com/file/d/190jmpbyQXI5poz7KYdA0jSfDoIZdTeC1/view?usp=sharing 

## Notes

- Keep this README clear and to the point.
- Do not leave placeholders — fill in your actual details.
