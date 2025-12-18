# Mini-Assignment #2: Backend Integration & Role-Based Access Control

**COM S 319 – Final Project – ISU Campus Explorer**

**Team Members:** Mekhi San (Member 1), Ash Bhuiyan (Member 2)

---

## Feature Branch

Feature branch for this mini-assignment:  
Mekhi San Member1 (sanm20): https://git.las.iastate.edu/se-coms-3190/fall-2025/final-project/NM_1/-/tree/8-member1-nav-login-campus-map-ui?ref_type=heads 

Ash Bhuiyan Member2 (mbhuiyan): https://git.las.iastate.edu/se-coms-3190/fall-2025/finalproject/NM_1/-/tree/9-member2-express-api-mock-auth-student-dashboard-approuting?ref_type=heads 

---

## 1. New Functional Pages

### Member 1 (Mekhi San): Student Dashboard

**Page:** `frontend/src/pages/StudentDashboard.jsx`

**Purpose:** Provides a personalized dashboard for students to explore campus buildings, view recommendations, and interact with building data from the backend.

**Key Features:**
- **Authentication Check:** Requires user login; shows login prompt if not authenticated
- **Admin Redirect:** If an admin accesses this page, shows a message directing them to the Admin Dashboard
- **Recommended Buildings Section:** Displays the first 3 academic buildings from the backend as personalized recommendations
- **Interactive Building Explorer:**
  - Type filter dropdown (All, Academic, Administration, Student Life, Athletics, Residence, Landmark)
  - Toggle to show only academic buildings
  - Real-time filtering of buildings fetched from `/api/buildings`
- **Building Detail Panel:** 
  - Click any building to view detailed information
  - Shows building code, type, description, year built, floors, capacity, hours, and departments
  - Link to view building on the Campus Map
- **Backend Integration:** Fetches building data from `GET /api/buildings` on component mount

**Interactions:**
- Filter buildings by type using dropdown
- Toggle academic-only view with checkbox
- Click building cards to view detailed information
- Navigate to Campus Map from building details

---

### Member 2 (Ash Bhuiyan): Admin Dashboard

**Page:** `frontend/src/pages/AdminDashboard.jsx`

**Purpose:** Provides administrative interface for managing campus data, viewing system statistics, and accessing admin-only features.

**Key Features:**
- **Role-Based Access Control:** 
  - Shows "Not Authorized" message if user is not authenticated or not an admin
  - Displays admin dashboard only when `auth.role === 'admin'`
- **Statistics Cards:**
  - Total Registered Users (1,250)
  - Campus Buildings (10)
  - Active Tours (8)
- **Admin Capabilities Section:** Lists future management features:
  - Building data management via `/api/buildings` endpoint (CRUD operations)
  - User account and role management
  - System health monitoring
  - Campus tour schedule configuration
- **Backend Integration Section:** Documents available API endpoints for future CRUD operations

**Interactions:**
- View system statistics
- Access documentation about backend endpoints
- Navigate to other admin features (prepared for future expansion)

---

## 2. Backend Setup

### Express Server

**Location:** `backend/server.js`

**Configuration:**
- Port: `3000`
- CORS enabled for `http://localhost:5173` (Vite dev server)
- JSON body parsing middleware

**Endpoints:**

#### `GET /api/health`
- **Purpose:** Health check endpoint
- **Response:**
  ```json
  {
    "status": "ok",
    "message": "Backend is running"
  }
  ```

#### `GET /api/buildings`
- **Purpose:** Retrieve all ISU campus buildings
- **Response:**
  ```json
  {
    "success": true,
    "buildings": [
      {
        "id": "library",
        "name": "Parks Library",
        "code": "LIB",
        "type": "Academic",
        "departments": ["Library Services", "Research Support", "Study Spaces"],
        "description": "...",
        "hours": "Mon-Thu: 7:30am-2:00am, Fri: 7:30am-10:00pm",
        "capacity": "2,500 students",
        "yearBuilt": "1925",
        "floors": 4
      },
    ]
  }
  ```
- **Mock Data:** Returns 10 realistic ISU buildings including Parks Library, Beardshear Hall, Memorial Union, Campanile, Hilton Coliseum, Carver Hall, Friley Hall, Coover Hall, Sukup Hall, and State Gymnasium

#### `POST /auth/login`
- **Purpose:** Authenticate users and return role information
- **Request Body:**
  ```json
  {
    "email": "admin@iastate.edu",
    "password": "admin123"
  }
  ```
- **Success Response (200):**
  ```json
  {
    "success": true,
    "role": "admin" | "user",
    "name": "Admin User",
    "email": "admin@iastate.edu"
  }
  ```
- **Error Response (401):**
  ```json
  {
    "success": false,
    "message": "Invalid email or password"
  }
  ```
- **Hardcoded Users:**
  - Admin: `admin@iastate.edu` / `admin123` / role: `admin`
  - Student 1: `student1@iastate.edu` / `student123` / role: `user`
  - Student 2: `student2@iastate.edu` / `student123` / role: `user`

**Package Configuration:** `backend/package.json`
- Dependencies: `express`, `cors`
- Dev Dependencies: `nodemon`
- Scripts: `npm start` (production), `npm run dev` (development with auto-reload)

---

## 3. Frontend–Backend Interaction

### Components Calling Backend Endpoints

1. **CampusMap Component** (`frontend/src/pages/CampusMap.jsx`)
   - **Endpoint:** `GET /api/buildings`
   - **When:** On component mount (`useEffect`)
   - **Data Flow:** 
     - Fetches building data from backend
     - Merges with visual position data for map rendering
     - Updates state with `buildings`, `loading`, and `error`
     - Displays buildings on interactive SVG map
     - Shows loading message while fetching
     - Displays error message if fetch fails

2. **Login Component** (`frontend/src/pages/Login.jsx`)
   - **Endpoint:** `POST /auth/login`
   - **When:** On form submission
   - **Data Flow:**
     - Sends email and password to backend
     - Receives `role`, `name`, and `email` on success
     - Calls `onLogin` callback to update global auth state
     - Redirects based on role:
       - Admin → `/admin`
       - User → `/student`
     - Shows error message on authentication failure

3. **StudentDashboard Component** (`frontend/src/pages/StudentDashboard.jsx`)
   - **Endpoint:** `GET /api/buildings`
   - **When:** On component mount
   - **Data Flow:**
     - Fetches all buildings from backend
     - Filters and displays recommended academic buildings
     - Provides interactive filtering by type
     - Shows detailed building information on selection

### Data Flow Summary

```
Backend (Express)          Frontend (React)
─────────────────          ─────────────────
/api/buildings      →      CampusMap.jsx
                          StudentDashboard.jsx

/auth/login         ←      Login.jsx
                    →      App.jsx (global auth state)
                    →      NavBar.jsx (role-based UI)
                    →      AdminDashboard.jsx (access control)
                    →      StudentDashboard.jsx (access control)
```

---

## 4. Role-Based Behavior

### Global Auth State

**Location:** `frontend/src/App.jsx`

**State Structure:**
```javascript
{
  isAuthenticated: boolean,
  role: 'admin' | 'user' | null,
  name: string | null,
  email: string | null
}
```

**Functions:**
- `handleLogin({ role, name, email })`: Sets authenticated state
- `handleLogout()`: Resets to unauthenticated state

**State Propagation:** Auth state is passed as `auth` prop to all page components and NavBar

### NavBar Role-Based Changes

**Location:** `frontend/src/components/NavBar.jsx`

**Always Visible:**
- Home link (`/`)
- Campus Map link (`/campus`)

**When Authenticated:**
- Greeting: "Hi, {auth.name || auth.email}"
- Logout button

**When `auth.role === 'admin'`:**
- "Admin" link → `/admin`

**When `auth.role === 'user'`:**
- "My Dashboard" link → `/student`

**When Not Authenticated:**
- "Login" link → `/login`
- "Sign Up" link → `/signup`

### Admin-Only Route Protection

**AdminDashboard.jsx:**
- Checks `auth.isAuthenticated` and `auth.role === 'admin'`
- If not authorized, displays:
  - Heading: "Not Authorized"
  - Message explaining admin-only access
  - Buttons to go to Login or Home

### Student Dashboard Access Control

**StudentDashboard.jsx:**
- Requires authentication (shows login prompt if not authenticated)
- If admin accesses, shows message directing to Admin Dashboard
- Only regular users (`role === 'user'`) see full student dashboard

### Signup Restriction

**Signup.jsx:**
- Displays prominent notice that admin accounts are pre-created in backend
- Clarifies that signup is for non-admin students only
- Visual warning box with cardinal red styling

---

## 5. Evidence (Screenshots / Recordings)
  - Added in the pdf "Mini-Assignment-2"

### Frontend–Backend Communication Evidence

#### Required Evidence:

1. **Admin Login → Admin Dashboard Redirect:**
   - Screenshot or screen recording showing:
     - Login page with admin credentials (`admin@iastate.edu` / `admin123`)
     - Successful login redirect to `/admin` (Admin Dashboard)
     - Admin Dashboard displaying statistics and capabilities
   - *Location: Add screenshot/recording link here*

2. **Student Login → Student Dashboard Redirect:**
   - Screenshot or screen recording showing:
     - Login page with student credentials (`student1@iastate.edu` / `student123`)
     - Successful login redirect to `/student` (Student Dashboard)
     - Student Dashboard displaying recommended buildings
   - *Location: Add screenshot/recording link here*

3. **Backend API Integration:**
   - Screenshot or screen recording showing:
     - CampusMap or StudentDashboard making a request to `/api/buildings`
     - Browser DevTools Network tab showing the API request/response
     - Buildings displayed on the page from the backend data
   - *Location: Add screenshot/recording link here*

### Additional Screenshots to Capture:

1. **Admin Login Flow:**
   - Screenshot: Login page with admin credentials entered
   - Screenshot: Successful login redirect to Admin Dashboard
   - Screenshot: Admin Dashboard showing statistics and capabilities

2. **Student Login Flow:**
   - Screenshot: Login page with student credentials entered
   - Screenshot: Successful login redirect to Student Dashboard
   - Screenshot: Student Dashboard showing recommended buildings and filters

3. **Access Control:**
   - Screenshot: "Not Authorized" message when non-admin tries to access `/admin`
   - Screenshot: Login prompt when unauthenticated user tries to access `/student`

4. **Backend Integration:**
   - Screenshot: CampusMap populated with buildings from `/api/buildings`
   - Screenshot: Student Dashboard showing filtered buildings from backend
   - Screenshot: Building detail panel with data from backend

5. **Role-Based Navigation:**
   - Screenshot: NavBar showing "Admin" link for admin users
   - Screenshot: NavBar showing "My Dashboard" link for student users
   - Screenshot: NavBar showing "Login" and "Sign Up" for unauthenticated users

6. **Signup Restriction:**
   - Screenshot: Signup page with admin account restriction notice

### Screen Recording Suggestions:

- **Full Authentication Flow:** Login as admin → view Admin Dashboard → logout → login as student → view Student Dashboard
- **Access Control Demo:** Try accessing `/admin` as student → see "Not Authorized" → login as admin → access granted
- **Backend Integration:** Show CampusMap loading buildings from backend → filter buildings → view details

---

## Technical Notes

### Development Setup

**Backend:**
```bash
cd backend
npm install
npm run dev  # Runs on http://localhost:3000
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev  # Runs on http://localhost:5173
```

### Proxy Configuration

The Vite dev server (`frontend/vite.config.js`) is configured to proxy API requests:
- `/api/*` → `http://localhost:3000`
- `/auth/*` → `http://localhost:3000`

This allows frontend code to use relative URLs without hardcoding localhost.

### File Ownership

**Member 1 (Mekhi San):**
- `frontend/src/pages/Login.jsx` (updated)
- `frontend/src/pages/Signup.jsx` (updated)
- `frontend/src/pages/StudentDashboard.jsx` (new)
- `frontend/src/components/NavBar.jsx` (updated)
- `frontend/src/main.jsx` (existing)

**Member 2 (Ash Bhuiyan):**
- `frontend/src/pages/Home.jsx` (existing)
- `frontend/src/pages/CampusMap.jsx` (updated)
- `frontend/src/pages/AdminDashboard.jsx` (new)
- `frontend/src/styles.css` (existing)

**Shared:**
- `frontend/src/App.jsx` (updated with auth state)
- `backend/server.js` (new)
- `backend/package.json` (new)
- `frontend/vite.config.js` (updated with proxy)

---

## Summary

Mini-Assignment #2 successfully implements:
- Backend Express server with health, buildings, and login endpoints
- Frontend–backend communication via fetch API
- Global auth state management in App.jsx
- Role-based access control (admin vs user)
- Admin Dashboard (Member 2) with admin-only protection
- Student Dashboard (Member 1) with interactive building exploration
- Role-based NavBar navigation
- Login integration with backend authentication
- CampusMap fetching from backend
- Signup restriction notice for admin accounts

All features are functional and ready for testing. The application maintains the existing design and styling from Mini-Assignment #1 while adding the new backend integration and role-based features.
