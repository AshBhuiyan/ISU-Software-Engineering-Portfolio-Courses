import React, { useState } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import Signup from './pages/Signup.jsx'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import CampusMap from './pages/CampusMap.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import AdminBuildings from './pages/AdminBuildings.jsx'
import StudentDashboard from './pages/StudentDashboard.jsx'
import TourPlanner from './pages/TourPlanner.jsx'
import MyTours from './pages/MyTours.jsx'
import TourConfirmation from './pages/TourConfirmation.jsx'
import About from './pages/About.jsx'
import BuildingDetail from './pages/BuildingDetail.jsx'

export default function App() {
  const [auth, setAuth] = useState({
    isAuthenticated: false,
    role: null,
    name: null,
    email: null,
  })

  const handleLogin = ({ role, name, email }) => {
    setAuth({
      isAuthenticated: true,
      role,
      name,
      email,
    })
  }

  const handleLogout = () => {
    setAuth({
      isAuthenticated: false,
      role: null,
      name: null,
      email: null,
    })
  }

  return (
    <div className="app-shell">
      <div className="backdrop" aria-hidden="true"></div>
      <NavBar auth={auth} onLogout={handleLogout} />
      <main role="main" className="container">
        <Routes>
          <Route path="/" element={<Home auth={auth} />} />
          <Route path="/campus" element={<CampusMap auth={auth} />} />
          <Route path="/login" element={<Login onLogin={handleLogin} auth={auth} />} />
          <Route path="/signup" element={<Signup auth={auth} onLogin={handleLogin} />} />
          <Route path="/admin" element={<AdminDashboard auth={auth} />} />
          <Route path="/admin/buildings" element={<AdminBuildings auth={auth} />} />
          <Route path="/student" element={<StudentDashboard auth={auth} />} />
          <Route path="/tours" element={<MyTours auth={auth} />} />
          <Route path="/tours/create" element={<TourPlanner auth={auth} />} />
          <Route path="/tours/confirmation" element={<TourConfirmation auth={auth} />} />
          <Route path="/about" element={<About />} />
          <Route path="/building/:id" element={<BuildingDetail auth={auth} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="footer" aria-label="Site footer">
        <div style={{maxWidth: '1200px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem'}}>
          <p>© {new Date().getFullYear()} Iowa State University Campus Explorer</p>
          <div style={{display: 'flex', gap: '2rem', fontSize: '0.875rem'}}>
            <a href="#" onClick={(e)=>e.preventDefault()} style={{color: 'var(--text-muted)', textDecoration: 'none'}}>Privacy Policy</a>
            <a href="#" onClick={(e)=>e.preventDefault()} style={{color: 'var(--text-muted)', textDecoration: 'none'}}>Accessibility</a>
            <a href="#" onClick={(e)=>e.preventDefault()} style={{color: 'var(--text-muted)', textDecoration: 'none'}}>Contact</a>
          </div>
        </div>
      </footer>
    </div>
  )
}
