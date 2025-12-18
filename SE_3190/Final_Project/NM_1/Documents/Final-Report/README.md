# Final Project – ISU Campus Explorer

## Table of Contents

- [Introduction](#introduction)
- [Project Description](#project-description)
- [File Structure](#file-structure)
- [Code & Logic](#code--logic)
- [Screenshots](#screenshots)
- [Setup](#setup)
- [Contributions](#contributions)
- [API Setup](#api-setup)

## Introduction

**ISU Campus Explorer** is a full-stack web application designed to help Iowa State University students, faculty, and visitors navigate and explore the campus. The application provides an interactive campus map, detailed building information, floor plans, and a custom tour planning system.

**Problem:** Navigating a large university campus can be overwhelming for new students and visitors. Finding specific buildings, understanding their layouts, and planning efficient routes between locations requires multiple resources and can be time-consuming.

**Purpose:** To create a centralized, user-friendly platform that consolidates campus building information, provides interactive floor plans, and enables users to create personalized campus tours.

**Users:** 
- **Students:** Need to find classrooms, study spaces, and navigate between buildings
- **Visitors:** Want to explore campus landmarks and plan tours
- **Administrators:** Manage building information and content

**Goals:**
- Provide an intuitive campus map with building visualization
- Offer detailed building information including floor plans and room search
- Enable users to create and save custom campus tours
- Support role-based access (students vs. administrators)
- Maintain a clean, responsive UI with ISU branding

**Inspiration:** This project is original but inspired by real-world campus navigation apps and tour planning tools. It incorporates features from the midterm project (interactive floor maps) and extends them with a full-stack architecture.

## Project Description

### Main Features

1. **Interactive Campus Map**
   - Visual representation of campus buildings
   - Clickable building markers with hover effects
   - Building filtering by type (Academic, Administration, Student Life, Athletics, Residence, Landmark)
   - Search functionality to find specific buildings
   - Drag-and-drop building repositioning for admins

2. **Building Detail Pages**
   - Comprehensive building information (hours, capacity, year built, departments)
   - Hero images and video support (including YouTube embeds)
   - Image galleries
   - Interactive floor plans with room number search
   - Automatic floor selection based on room numbers (e.g., room 205 → Level 2)

3. **Tour Planning System** (Advanced Feature)
   - Create custom campus tours by selecting buildings
   - Drag-to-reorder tour stops
   - Visual tour routing on campus map with SVG polylines
   - Numbered badges and directional arrows showing tour path
   - Save and manage multiple tours
   - Tour confirmation page

4. **User Authentication & Role-Based Access**
   - User registration and login
   - Role-based dashboards (Student vs. Admin)
   - Admin-only building management (CRUD operations)
   - Secure session management

5. **Admin Building Management**
   - Create, read, update, and delete buildings
   - Upload building images and videos
   - Manage floor plan images (up to 10 floors per building)
   - Set default hero images for new buildings

### User Flow

1. **Unauthenticated Users:**
   - Browse campus map
   - View building details
   - Access about page
   - Sign up or log in to access additional features

2. **Students:**
   - Access student dashboard
   - Explore buildings with filtering
   - Create and manage custom tours
   - View saved tours

3. **Administrators:**
   - Access admin dashboard with statistics
   - Manage buildings (CRUD operations)
   - Upload and edit building media (images, videos, floor plans)
   - View system-wide data

### CRUD Operations

**Buildings (Entity #1):**
- **Create:** Admin can add new buildings via `/admin/buildings`
- **Read:** All users can view buildings on map and detail pages
- **Update:** Admin can edit building information and media
- **Delete:** Admin can remove buildings

**Tours (Entity #2):**
- **Create:** Authenticated users can create tours via `/tours/create`
- **Read:** Users can view their saved tours at `/tours`
- **Update:** Users can modify tour order and details
- **Delete:** Users can remove tours

## File Structure

```
NM_1/
├── frontend/                    # React + Vite frontend
│   ├── src/
│   │   ├── components/
│   │   │   └── NavBar.jsx      # Navigation component
│   │   ├── pages/
│   │   │   ├── Home.jsx        # Landing page
│   │   │   ├── CampusMap.jsx   # Interactive campus map
│   │   │   ├── BuildingDetail.jsx  # Building detail with floor plans
│   │   │   ├── Login.jsx       # User login
│   │   │   ├── Signup.jsx      # User registration
│   │   │   ├── AdminDashboard.jsx  # Admin dashboard
│   │   │   ├── AdminBuildings.jsx  # Building CRUD management
│   │   │   ├── StudentDashboard.jsx  # Student dashboard
│   │   │   ├── TourPlanner.jsx  # Tour creation
│   │   │   ├── MyTours.jsx     # Tour management
│   │   │   ├── TourConfirmation.jsx  # Tour confirmation
│   │   │   └── About.jsx       # About/Team page
│   │   ├── App.jsx             # Main app component with routing
│   │   ├── main.jsx            # React entry point
│   │   └── styles.css          # Global styles
│   ├── public/
│   │   └── assets/
│   │       ├── images/         # Static images
│   │       ├── videos/         # Static videos
│   │       └── uploads/        # User-uploaded files
│   ├── package.json
│   └── vite.config.js          # Vite configuration with proxy
│
├── backend/                     # Node.js + Express backend
│   ├── config/
│   │   └── db.js               # MongoDB connection
│   ├── models/
│   │   ├── Building.js         # Building schema
│   │   ├── Tour.js             # Tour schema
│   │   └── User.js             # User schema
│   ├── routes/
│   │   ├── buildings.js        # Building CRUD routes
│   │   ├── tours.js            # Tour CRUD routes
│   │   ├── auth.js             # Authentication routes
│   │   └── upload.js           # File upload route
│   ├── scripts/
│   │   ├── seedBuildings.js    # Seed building data
│   │   └── seedUsers.js        # Seed user data
│   ├── server.js               # Express server entry point
│   └── package.json
│
└── Documents/                   # Project documentation
    ├── Final-Report/
    │   └── README.md           # This file
    ├── Final-Project-Summary.md



## Code & Logic

### Frontend-Backend Communication

The frontend communicates with the backend through RESTful API calls:

```javascript
// Example: Fetching buildings
const response = await fetch('/api/buildings')
const data = await response.json()
if (data.success) {
  setBuildings(data.buildings)
}
```

**Proxy Configuration:** Vite is configured to proxy `/api` and `/auth` requests to the backend server:

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': 'http://localhost:3000',
    '/auth': 'http://localhost:3000'
  }
}
```

### Database Usage

**MongoDB with Mongoose** is used for data persistence:

```javascript
// Example: Building Model
const buildingSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  name: { type: String, required: true },
  // ... other fields
  floorPlans: { type: [String], default: [] }
})
```

**Database Models:**
- **Building:** Stores campus building information, images, videos, and floor plans
- **Tour:** Stores user-created tours with building references
- **User:** Stores user accounts with email, password (hashed), and role

### Code Snippets

**Building Detail with Room Search:**
```javascript
// BuildingDetail.jsx - Room search logic
const handleRoomInput = () => {
  const room = parseInt(roomInput.trim())
  // Determine floor: room 050 → floor 0, room 150 → floor 1, etc.
  const floorIndex = Math.floor(room / 100)
  const floorKey = floorIndex === 0 ? 'lower' : `level${floorIndex}`
  setSelectedFloor(floorKey)
}
```

**Tour Route Visualization:**
```javascript
// CampusMap.jsx - SVG polyline for tour route
<polyline
  points={tourPoints.map(p => `${p.x},${p.y}`).join(' ')}
  fill="none"
  stroke="var(--cardinal)"
  strokeWidth="3"
  markerEnd="url(#arrowhead)"
/>
```

**File Upload Handler:**
```javascript
// BuildingDetail.jsx - Floor plan upload
const formData = new FormData()
formData.append('file', file)
const response = await fetch('/api/upload', {
  method: 'POST',
  body: formData
})
```

## Screenshots

### Screenshot 1: Interactive Campus Map
**Description:** The main campus map page showing an interactive SVG visualization of campus buildings. Buildings are displayed as colored rectangles with hover effects. Users can click buildings to view details, filter by type, and search for specific buildings. The map includes a visual tour route overlay when viewing saved tours.

### Screenshot 2: Building Detail with Floor Plans
**Description:** A building detail page showing comprehensive information including hero image/video, building description, hours, capacity, and departments. The page features an interactive floor plan section where users can search for rooms by number (e.g., "205") and automatically view the corresponding floor plan. Floor selection buttons allow manual navigation between levels.

### Screenshot 3: Tour Planner Interface
**Description:** The tour creation page where users can select buildings from a list, add them to their tour, and reorder stops using up/down buttons. The interface shows a preview of selected buildings and allows users to name their tour before saving. The page integrates with the campus map to visualize the tour route.

### Screenshot 4: Admin Building Management
**Description:** The admin dashboard showing building management interface. Admins can create, edit, and delete buildings. The edit media modal allows uploading hero images, videos, and floor plan images (up to 10 floors). The interface includes form validation and error handling for all CRUD operations.

## Setup

### Prerequisites

- **Node.js** (v16 or higher)
- **MongoDB** (local installation or MongoDB Atlas account)
- **npm** or **yarn** package manager

### Step-by-Step Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd NM_1
   ```

2. **Backend Setup:**
   ```bash
   cd backend
   npm install
   ```

3. **Create `.env` file in `backend/` directory:**
   ```env
   MONGO_URI=mongodb://localhost:27017/isu-campus-explorer
   ```
   Or for MongoDB Atlas:
   ```env
   MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/isu-campus-explorer?retryWrites=true&w=majority
   ```

4. **Seed the database:**
   ```bash
   # Seed buildings
   node scripts/seedBuildings.js
   
   # Seed default users (admin and test students)
   node scripts/seedUsers.js
   ```

5. **Start the backend server:**
   ```bash
   npm start
   # Or for development with auto-reload:
   npm run dev
   ```
   Backend runs on: `http://localhost:3000`

6. **Frontend Setup (in a new terminal):**
   ```bash
   cd frontend
   npm install
   ```

7. **Start the frontend development server:**
   ```bash
   npm run dev
   ```
   Frontend runs on: `http://localhost:5173`

8. **Access the application:**
   - Open `http://localhost:5173` in your browser
   - Default admin credentials (after seeding):
     - Email: `admin@isu.edu`
     - Password: `admin123`

### Environment Variables

**Backend `.env` file:**
- `MONGO_URI`: MongoDB connection string (required)

**Note:** The frontend uses Vite's proxy configuration to communicate with the backend, so no frontend environment variables are needed.

## Contributions

### Member 1 (Mekhi San)
- **Frontend Development:**
  - Navigation bar component with role-based links
  - Campus map page with interactive building visualization
  - Building detail page with floor plans and room search
  - Home page with hero section and feature cards
  - About page with team information
  - Responsive UI/UX design and styling

- **Backend Development:**
  - Building model and CRUD routes
  - File upload system for images and videos
  - Floor plan management system
  - Database seeding scripts

- **Features:**
  - Interactive campus map with SVG visualization
  - Building filtering and search
  - Floor plan viewer with room number search
  - Media upload and management (images, videos, floor plans)
  - Admin building CRUD interface

### Member 2 (Ash Bhuiyan)
- **Frontend Development:**
  - User authentication (login/signup pages)
  - Student dashboard with building explorer
  - Admin dashboard with statistics
  - Tour planner interface
  - Tour management page (My Tours)
  - Tour confirmation page

- **Backend Development:**
  - User model and authentication routes
  - Tour model and CRUD routes
  - Tour routing visualization logic
  - Session management

- **Features:**
  - User registration and login system
  - Role-based access control
  - Tour creation and management
  - Visual tour routing on campus map
  - Student and admin dashboards

## API Setup

This project does not use external APIs. All functionality is built using:

- **MongoDB** (database) - No API key required for local installation
- **MongoDB Atlas** (optional cloud database) - Free tier available

### MongoDB Setup (Local)

1. **Install MongoDB:**
   - Download from [mongodb.com](https://www.mongodb.com/try/download/community)
   - Follow installation instructions for your operating system
   - Start MongoDB service

2. **Connection String:**
   - Local: `mongodb://localhost:27017/isu-campus-explorer`
   - No authentication required for local development

### MongoDB Atlas Setup (Cloud - Optional)

1. **Sign up:**
   - Go to [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
   - Create a free account

2. **Create a cluster:**
   - Choose the free M0 tier
   - Select a cloud provider and region
   - Wait for cluster creation (2-3 minutes)

3. **Get connection string:**
   - Click "Connect" on your cluster
   - Choose "Connect your application"
   - Copy the connection string
   - Replace `<password>` with your database user password
   - Replace `<dbname>` with `isu-campus-explorer`

4. **Add to `.env`:**
   ```env
   MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/isu-campus-explorer?retryWrites=true&w=majority
   ```

5. **Network Access:**
   - Go to "Network Access" in Atlas
   - Add IP address `0.0.0.0/0` to allow connections from anywhere (for development)
   - For production, use specific IP addresses

**Note:** MongoDB Atlas free tier includes:
- 512 MB storage
- Shared RAM
- No credit card required
- Suitable for development and small projects

---

## Additional Notes

- **Default Admin Account:** After seeding, use `admin@isu.edu` / `admin123` to access admin features
- **File Uploads:** Uploaded files are stored in `frontend/public/assets/uploads/`
- **Development Mode:** Use `npm run dev` in both frontend and backend for hot-reload during development
- **Production Build:** Run `npm run build` in the frontend directory to create a production build
