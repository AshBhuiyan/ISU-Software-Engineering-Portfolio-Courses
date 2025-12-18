import React from 'react'
import { NavLink, Link } from 'react-router-dom'

export default function NavBar({ auth, onLogout }){
  const handleLogout = () => {
    if (onLogout) {
      onLogout()
    }
  }

  return (
    <header className="topbar" role="banner">
      <nav className="nav" aria-label="Primary Navigation">
        <Link className="brand" to="/" aria-label="Iowa State University Campus Explorer Home">
          <span className="cyclone">🏛️</span> ISU Campus Explorer
        </Link>
        <ul className="nav-links">
          <li><NavLink to="/" className={({isActive}) => isActive ? 'active' : ''}>Home</NavLink></li>
          <li><NavLink to="/campus" className={({isActive}) => isActive ? 'active' : ''}>Campus Map</NavLink></li>
          <li><NavLink to="/about" className={({isActive}) => isActive ? 'active' : ''}>About</NavLink></li>
          
          {auth.isAuthenticated ? (
            <>
              <li><NavLink to="/tours" className={({isActive}) => isActive ? 'active' : ''}>My Tours</NavLink></li>
              {auth.role === 'admin' && (
                <li><NavLink to="/admin" className={({isActive}) => isActive ? 'active' : ''}>Admin</NavLink></li>
              )}
              {auth.role === 'user' && (
                <li><NavLink to="/student" className={({isActive}) => isActive ? 'active' : ''}>My Dashboard</NavLink></li>
              )}
              <li style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.75rem 1.25rem' }}>
                <span className="muted" style={{ fontSize: '0.9rem' }}>
                  Hi, {auth.name || auth.email}
                </span>
              </li>
              <li>
                <button 
                  onClick={handleLogout}
                  className="btn ghost"
                  style={{ border: 'none', background: 'transparent', padding: '0.75rem 1.25rem' }}
                >
                  Logout
                </button>
              </li>
            </>
          ) : (
            <>
              <li><NavLink to="/login" className={({isActive}) => isActive ? 'active' : ''}>Login</NavLink></li>
              <li><NavLink to="/signup" className={({isActive}) => isActive ? 'active' : ''}>Sign Up</NavLink></li>
            </>
          )}
        </ul>
      </nav>
    </header>
  )
}
