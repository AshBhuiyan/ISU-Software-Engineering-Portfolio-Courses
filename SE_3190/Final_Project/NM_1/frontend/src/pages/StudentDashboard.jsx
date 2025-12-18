import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function StudentDashboard({ auth }) {
  const navigate = useNavigate()
  const [buildings, setBuildings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedBuilding, setSelectedBuilding] = useState(null)
  const [typeFilter, setTypeFilter] = useState('All')
  const [searchQuery, setSearchQuery] = useState('')

  // Fetch buildings from backend
  useEffect(() => {
    const fetchBuildings = async () => {
      try {
        setLoading(true)
        const response = await fetch('/api/buildings')
        if (!response.ok) {
          throw new Error('Failed to fetch buildings')
        }
        const data = await response.json()
        if (data.success) {
          setBuildings(data.buildings)
        } else {
          throw new Error('Invalid response format')
        }
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    fetchBuildings()
  }, [])

  // Show login prompt if not authenticated
  if (!auth.isAuthenticated) {
    return (
      <section className="centered">
        <div className="card form-card" role="region" aria-labelledby="login-required-title">
          <h1 id="login-required-title" className="h1" style={{ color: 'var(--cardinal)' }}>Login Required</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            You must be logged in to view your student dashboard. Please sign in to access personalized features and building recommendations.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/login" className="btn primary">
              Go to Login
            </Link>
            <Link to="/" className="btn secondary">
              Return to Home
            </Link>
          </div>
        </div>
      </section>
    )
  }

  // Note: Admins can view student dashboard but with a notice

  // Filter buildings based on type filter and search query
  const filteredBuildings = buildings.filter(building => {
    // Filter by type
    if (typeFilter !== 'All' && building.type !== typeFilter) {
      return false
    }
    
    // Filter by search query (name, code, or departments)
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase().trim()
      const matchesName = building.name.toLowerCase().includes(query)
      const matchesCode = building.code?.toLowerCase().includes(query)
      const matchesDescription = building.description?.toLowerCase().includes(query)
      const matchesDepartment = building.departments?.some(dept => 
        dept.toLowerCase().includes(query)
      )
      
      if (!matchesName && !matchesCode && !matchesDescription && !matchesDepartment) {
        return false
      }
    }
    
    return true
  })

  // Get recommended buildings (first 3 academic buildings)
  const recommendedBuildings = buildings
    .filter(b => b.type === 'Academic')
    .slice(0, 3)

  // Get unique building types
  const buildingTypes = ['All', ...new Set(buildings.map(b => b.type))]

  return (
    <section className="campus">
      <header className="campus-header">
        <h1 className="display">Student Dashboard</h1>
        {auth.role === 'admin' && (
          <div style={{ 
            background: 'rgba(200,16,46,0.1)', 
            border: '1px solid var(--cardinal)', 
            borderRadius: '0.5rem', 
            padding: '1rem', 
            marginBottom: '1rem'
          }}>
            <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--cardinal)', fontWeight: '600' }}>
              👤 Admin View: You are viewing the student experience. <Link to="/admin" style={{color: 'var(--cardinal)', textDecoration: 'underline'}}>Go to Admin Dashboard</Link>
            </p>
          </div>
        )}
        <p className="muted">Welcome, {auth.name || 'Student'}. Explore recommended buildings and manage your campus preferences.</p>
      </header>

      {loading && (
        <div className="centered">
          <p className="muted">Loading building data...</p>
        </div>
      )}

      {error && (
        <div className="card" style={{ background: '#fee', borderColor: '#fcc' }}>
          <p style={{ color: '#c33', margin: 0 }}>Error: {error}</p>
        </div>
      )}

      {!loading && !error && (
        <>
          {/* Recommended Buildings Section */}
          <div className="card" style={{ marginTop: '2rem', marginBottom: '2rem' }}>
            <h2 className="h2">📚 Recommended Academic Buildings</h2>
            <p className="muted" style={{ marginBottom: '1rem' }}>
              Based on your student profile, here are some buildings you might find useful:
            </p>
            <div className="grid features" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', marginTop: '1rem' }}>
              {recommendedBuildings.map(building => (
                <div 
                  key={building.id} 
                  className="card" 
                  style={{ cursor: 'pointer', padding: '1.5rem' }}
                  onClick={() => setSelectedBuilding(building)}
                >
                  <h3 className="h3" style={{ color: 'var(--cardinal)' }}>{building.name}</h3>
                  <p className="muted" style={{ fontSize: '0.9rem', marginTop: '0.5rem' }}>
                    {building.code} • {building.type}
                  </p>
                  <p className="muted" style={{ fontSize: '0.85rem', marginTop: '0.75rem' }}>
                    {building.description.substring(0, 100)}...
                  </p>
                </div>
              ))}
            </div>
          </div>

          {/* Interactive Building Explorer */}
          <div className="card" style={{ marginTop: '2rem', marginBottom: '2rem' }}>
            <h2 className="h2">🔍 Explore Campus Buildings</h2>
            
            {/* Enhanced Filter Controls */}
            <div style={{ marginTop: '1.5rem' }}>
              {/* Search Bar */}
              <div style={{ marginBottom: '1.5rem' }}>
                <label htmlFor="building-search" style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                  🔍 Search Buildings
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="building-search"
                    type="text"
                    placeholder="Search by name, code, or department (e.g., 'Library', 'LIB', 'Engineering')..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      paddingRight: searchQuery ? '3rem' : '1rem',
                      borderRadius: '0.5rem',
                      border: '1px solid var(--border)',
                      fontSize: '1rem',
                      transition: 'all 0.2s ease'
                    }}
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery('')}
                      style={{
                        position: 'absolute',
                        right: '0.5rem',
                        top: '50%',
                        transform: 'translateY(-50%)',
                        background: 'transparent',
                        border: 'none',
                        cursor: 'pointer',
                        fontSize: '1.2rem',
                        color: 'var(--text-muted)',
                        padding: '0.25rem',
                        display: 'flex',
                        alignItems: 'center'
                      }}
                      aria-label="Clear search"
                    >
                      ✕
                    </button>
                  )}
                </div>
              </div>

              {/* Filter Type Chips */}
              <div>
                <label style={{ display: 'block', marginBottom: '0.75rem', fontWeight: '600' }}>
                  Filter by Type:
                </label>
                <div style={{ 
                  display: 'flex', 
                  gap: '0.75rem', 
                  flexWrap: 'wrap',
                  marginBottom: '1rem'
                }}>
                  {buildingTypes.map(type => {
                    const isActive = typeFilter === type
                    const count = type === 'All' 
                      ? buildings.length 
                      : buildings.filter(b => b.type === type).length
                    
                    return (
                      <button
                        key={type}
                        onClick={() => setTypeFilter(type)}
                        style={{
                          padding: '0.5rem 1rem',
                          borderRadius: '1.5rem',
                          border: `2px solid ${isActive ? 'var(--cardinal)' : 'var(--border)'}`,
                          background: isActive ? 'var(--cardinal)' : 'var(--panel)',
                          color: isActive ? 'white' : 'var(--text)',
                          fontWeight: '600',
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                          transition: 'all 0.2s ease',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem'
                        }}
                        onMouseEnter={(e) => {
                          if (!isActive) {
                            e.target.style.background = 'var(--hover-bg)'
                            e.target.style.borderColor = 'var(--cardinal)'
                          }
                        }}
                        onMouseLeave={(e) => {
                          if (!isActive) {
                            e.target.style.background = 'var(--panel)'
                            e.target.style.borderColor = 'var(--border)'
                          }
                        }}
                      >
                        {type}
                        <span style={{
                          background: isActive ? 'rgba(255,255,255,0.3)' : 'var(--hover-bg)',
                          padding: '0.125rem 0.5rem',
                          borderRadius: '1rem',
                          fontSize: '0.75rem',
                          fontWeight: '700'
                        }}>
                          {count}
                        </span>
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Active Filters Summary & Clear Button */}
              {(typeFilter !== 'All' || searchQuery) && (
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '0.75rem 1rem',
                  background: 'rgba(200,16,46,0.05)',
                  borderRadius: '0.5rem',
                  border: '1px solid rgba(200,16,46,0.2)',
                  marginTop: '1rem'
                }}>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '0.875rem', fontWeight: '600', color: 'var(--cardinal)' }}>
                      Active Filters:
                    </span>
                    {typeFilter !== 'All' && (
                      <span style={{
                        padding: '0.25rem 0.75rem',
                        background: 'var(--cardinal)',
                        color: 'white',
                        borderRadius: '1rem',
                        fontSize: '0.75rem',
                        fontWeight: '600'
                      }}>
                        Type: {typeFilter} ✕
                      </span>
                    )}
                    {searchQuery && (
                      <span style={{
                        padding: '0.25rem 0.75rem',
                        background: 'var(--cardinal)',
                        color: 'white',
                        borderRadius: '1rem',
                        fontSize: '0.75rem',
                        fontWeight: '600'
                      }}>
                        Search: "{searchQuery}" ✕
                      </span>
                    )}
                    <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                      ({filteredBuildings.length} {filteredBuildings.length === 1 ? 'building' : 'buildings'})
                    </span>
                  </div>
                  <button
                    onClick={() => {
                      setTypeFilter('All')
                      setSearchQuery('')
                    }}
                    className="btn ghost"
                    style={{
                      padding: '0.5rem 1rem',
                      fontSize: '0.875rem'
                    }}
                  >
                    Clear All Filters
                  </button>
                </div>
              )}
            </div>

            {/* Building List */}
            <div style={{ marginTop: '2rem' }}>
              <h3 className="h3">Available Buildings ({filteredBuildings.length})</h3>
              <div style={{ display: 'grid', gap: '1rem', marginTop: '1rem' }}>
                {filteredBuildings.map(building => (
                  <div
                    key={building.id}
                    className="card"
                    style={{
                      cursor: 'pointer',
                      padding: '1.5rem',
                      border: selectedBuilding?.id === building.id ? '2px solid var(--cardinal)' : '1px solid var(--border)',
                      background: selectedBuilding?.id === building.id ? 'rgba(200,16,46,0.05)' : 'var(--card)'
                    }}
                    onClick={() => setSelectedBuilding(building)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '1rem' }}>
                      <div style={{ flex: '1' }}>
                        <h4 style={{ margin: 0, color: 'var(--cardinal)', fontSize: '1.25rem' }}>
                          {building.name}
                        </h4>
                        <p className="muted" style={{ marginTop: '0.25rem', fontSize: '0.9rem' }}>
                          {building.code} • {building.type}
                        </p>
                      </div>
                      <span
                        style={{
                          padding: '0.25rem 0.75rem',
                          borderRadius: '1rem',
                          fontSize: '0.75rem',
                          fontWeight: '600',
                          background: building.type === 'Academic' ? '#e3f2fd' : 
                                     building.type === 'Student Life' ? '#e8f5e8' :
                                     building.type === 'Athletics' ? '#fff3e0' : '#f5f5f5',
                          color: building.type === 'Academic' ? '#1976d2' :
                                 building.type === 'Student Life' ? '#388e3c' :
                                 building.type === 'Athletics' ? '#f57c00' : '#666'
                        }}
                      >
                        {building.type}
                      </span>
                    </div>
                    <p className="muted" style={{ marginTop: '0.75rem', fontSize: '0.9rem' }}>
                      {building.description}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Selected Building Detail Panel */}
          {selectedBuilding && (
            <div className="card" style={{ marginTop: '2rem', background: 'linear-gradient(135deg, rgba(200,16,46,0.05) 0%, rgba(241,190,72,0.05) 100%)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1rem' }}>
                <h2 className="h2" style={{ margin: 0 }}>Building Details</h2>
                <button
                  className="btn ghost"
                  onClick={() => setSelectedBuilding(null)}
                  style={{ padding: '0.5rem 1rem' }}
                >
                  ✕ Close
                </button>
              </div>
              
              <h3 className="h3" style={{ color: 'var(--cardinal)' }}>{selectedBuilding.name}</h3>
              <p className="muted" style={{ marginTop: '0.5rem' }}>
                <strong>Code:</strong> {selectedBuilding.code} | <strong>Type:</strong> {selectedBuilding.type}
              </p>
              
              <p className="muted" style={{ marginTop: '1rem' }}>
                {selectedBuilding.description}
              </p>

              <div className="building-info-grid" style={{ marginTop: '1.5rem' }}>
                <div className="info-item">
                  <strong>📅 Built:</strong>
                  <span>{selectedBuilding.yearBuilt}</span>
                </div>
                <div className="info-item">
                  <strong>🏢 Floors:</strong>
                  <span>{selectedBuilding.floors}</span>
                </div>
                <div className="info-item">
                  <strong>👥 Capacity:</strong>
                  <span>{selectedBuilding.capacity}</span>
                </div>
                <div className="info-item">
                  <strong>🕒 Hours:</strong>
                  <span>{selectedBuilding.hours}</span>
                </div>
              </div>

              <div style={{ marginTop: '1.5rem' }}>
                <strong>🏛️ Departments & Services:</strong>
                <div className="departments-list" style={{ marginTop: '0.75rem' }}>
                  {selectedBuilding.departments.map((dept, index) => (
                    <span key={index} className="department-tag">{dept}</span>
                  ))}
                </div>
              </div>

              <div style={{ marginTop: '1.5rem' }}>
                <Link to="/campus" className="btn primary">
                  View on Campus Map
                </Link>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  )
}

