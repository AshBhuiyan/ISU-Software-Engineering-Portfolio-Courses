import React, { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'

export default function TourPlanner({ auth }) {
  const navigate = useNavigate()
  const [buildings, setBuildings] = useState([])
  const [selectedBuildings, setSelectedBuildings] = useState([]) // Array of building IDs in order
  const [tourName, setTourName] = useState('')
  const [tourDescription, setTourDescription] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [typeFilter, setTypeFilter] = useState('All')

  // Redirect if not authenticated
  if (!auth.isAuthenticated) {
    return (
      <section className="centered">
        <div className="card form-card" role="region" aria-labelledby="login-required-title">
          <h1 id="login-required-title" className="h1" style={{ color: 'var(--cardinal)' }}>Login Required</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            You must be logged in to create a tour. Please sign in to access the tour planner.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/login" className="btn primary">Go to Login</Link>
            <Link to="/" className="btn secondary">Return to Home</Link>
          </div>
        </div>
      </section>
    )
  }

  // Fetch buildings
  useEffect(() => {
    fetchBuildings()
  }, [])

  const fetchBuildings = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await fetch('/api/buildings')
      if (!response.ok) {
        throw new Error('Failed to fetch buildings')
      }
      const data = await response.json()
      if (data.success && data.buildings) {
        setBuildings(data.buildings)
      } else {
        throw new Error('Invalid response format')
      }
    } catch (err) {
      setError(err.message)
      console.error('Error fetching buildings:', err)
    } finally {
      setLoading(false)
    }
  }

  // Filter buildings
  const filteredBuildings = buildings.filter(building => {
    const matchesSearch = building.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         building.code?.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = typeFilter === 'All' || typeFilter === building.type
    return matchesSearch && matchesType
  })

  // Get building types
  const buildingTypes = ['All', ...new Set(buildings.map(b => b.type))]

  // Add building to tour
  const handleAddBuilding = (buildingId) => {
    if (!selectedBuildings.includes(buildingId)) {
      setSelectedBuildings([...selectedBuildings, buildingId])
    }
  }

  // Remove building from tour
  const handleRemoveBuilding = (index) => {
    setSelectedBuildings(selectedBuildings.filter((_, i) => i !== index))
  }

  // Move building up in order
  const handleMoveUp = (index) => {
    if (index === 0) return
    const newOrder = [...selectedBuildings]
    ;[newOrder[index - 1], newOrder[index]] = [newOrder[index], newOrder[index - 1]]
    setSelectedBuildings(newOrder)
  }

  // Move building down in order
  const handleMoveDown = (index) => {
    if (index === selectedBuildings.length - 1) return
    const newOrder = [...selectedBuildings]
    ;[newOrder[index], newOrder[index + 1]] = [newOrder[index + 1], newOrder[index]]
    setSelectedBuildings(newOrder)
  }

  // Get building details by ID
  const getBuildingById = (id) => {
    return buildings.find(b => b.id === id)
  }

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault()
    setIsSubmitting(true)
    setSubmitError(null)

    // Validation
    if (!tourName.trim()) {
      setSubmitError('Tour name is required')
      setIsSubmitting(false)
      return
    }

    if (selectedBuildings.length === 0) {
      setSubmitError('Please add at least one building to your tour')
      setIsSubmitting(false)
      return
    }

    try {
      const response = await fetch('/api/tours', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          name: tourName.trim(),
          description: tourDescription.trim(),
          buildingIds: selectedBuildings,
          ownerEmail: auth.email
        })
      })

      const data = await response.json()

      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Failed to create tour')
      }

      // Navigate to confirmation page with tour data
      navigate('/tours/confirmation', { 
        state: { 
          tour: data.tour,
          mode: 'created'
        } 
      })
    } catch (err) {
      setSubmitError(err.message)
      console.error('Error creating tour:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="campus">
      <header className="campus-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 className="display">Tour Planner</h1>
            <p className="muted">Create your custom campus tour by selecting and ordering buildings to visit.</p>
          </div>
          <Link to="/tours" className="btn secondary">
            ← My Tours
          </Link>
        </div>
      </header>

      {error && (
        <div className="card" style={{ 
          background: 'linear-gradient(135deg, #fee 0%, #fdd 100%)', 
          borderColor: '#fcc', 
          marginTop: '2rem'
        }}>
          <p style={{ color: '#c33', margin: 0, fontWeight: '600' }}>⚠️ Error</p>
          <p style={{ color: '#c33', marginTop: '0.5rem', fontSize: '0.9rem' }}>{error}</p>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* Tour Details Form */}
        <div className="card" style={{ marginTop: '2rem' }}>
          <h2 className="h2">Tour Details</h2>
          
          <div style={{ marginTop: '1rem' }}>
            <label htmlFor="tourName">Tour Name *</label>
            <input
              id="tourName"
              type="text"
              value={tourName}
              onChange={(e) => setTourName(e.target.value)}
              placeholder="e.g., Engineering Buildings Tour"
              required
              maxLength={200}
            />
          </div>

          <div style={{ marginTop: '1rem' }}>
            <label htmlFor="tourDescription">Description (Optional)</label>
            <textarea
              id="tourDescription"
              value={tourDescription}
              onChange={(e) => setTourDescription(e.target.value)}
              placeholder="Describe your tour..."
              rows="3"
              maxLength={1000}
            />
          </div>
        </div>

        {/* Selected Buildings (Tour Stops) */}
        {selectedBuildings.length > 0 && (
          <div className="card" style={{ marginTop: '2rem' }}>
            <h2 className="h2">Tour Stops ({selectedBuildings.length})</h2>
            <p className="muted" style={{ marginTop: '0.5rem' }}>
              Reorder stops using the up/down buttons. Buildings will be visited in this order.
            </p>
            
            <div style={{ marginTop: '1.5rem', display: 'grid', gap: '1rem' }}>
              {selectedBuildings.map((buildingId, index) => {
                const building = getBuildingById(buildingId)
                if (!building) return null

                return (
                  <div 
                    key={`${buildingId}-${index}`}
                    className="card"
                    style={{ 
                      padding: '1rem',
                      background: 'rgba(200,16,46,0.05)',
                      border: '1px solid rgba(200,16,46,0.2)'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flex: 1 }}>
                        <div style={{
                          width: '2rem',
                          height: '2rem',
                          borderRadius: '50%',
                          background: 'var(--cardinal)',
                          color: 'white',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: '700',
                          flexShrink: 0
                        }}>
                          {index + 1}
                        </div>
                        <div style={{ flex: 1 }}>
                          <h3 style={{ margin: 0, color: 'var(--cardinal)' }}>{building.name}</h3>
                          <p className="muted" style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem' }}>
                            {building.code} • {building.type}
                          </p>
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: '0.5rem', flexShrink: 0 }}>
                        <button
                          type="button"
                          onClick={() => handleMoveUp(index)}
                          disabled={index === 0}
                          className="btn ghost"
                          style={{ padding: '0.5rem' }}
                          title="Move up"
                        >
                          ↑
                        </button>
                        <button
                          type="button"
                          onClick={() => handleMoveDown(index)}
                          disabled={index === selectedBuildings.length - 1}
                          className="btn ghost"
                          style={{ padding: '0.5rem' }}
                          title="Move down"
                        >
                          ↓
                        </button>
                        <button
                          type="button"
                          onClick={() => handleRemoveBuilding(index)}
                          className="btn ghost"
                          style={{ padding: '0.5rem', color: '#c33' }}
                          title="Remove"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {/* Building Selection */}
        <div className="card" style={{ marginTop: '2rem' }}>
          <h2 className="h2">Select Buildings</h2>
          
          {/* Search and Filter */}
          <div style={{ marginTop: '1rem', display: 'grid', gap: '1rem', gridTemplateColumns: '1fr auto' }}>
            <input
              type="text"
              placeholder="Search buildings..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ padding: '0.75rem' }}
            />
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              style={{ padding: '0.75rem' }}
            >
              {buildingTypes.map(type => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>

          {loading ? (
            <div style={{ textAlign: 'center', padding: '3rem' }}>
              <p className="muted">Loading buildings...</p>
            </div>
          ) : filteredBuildings.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '3rem' }}>
              <p className="muted">No buildings found matching your search.</p>
            </div>
          ) : (
            <div style={{ marginTop: '1.5rem', display: 'grid', gap: '1rem' }}>
              {filteredBuildings.map(building => {
                const isSelected = selectedBuildings.includes(building.id)
                return (
                  <div
                    key={building.id}
                    className="card"
                    style={{
                      padding: '1rem',
                      cursor: 'pointer',
                      border: isSelected ? '2px solid var(--cardinal)' : '1px solid var(--border)',
                      background: isSelected ? 'rgba(200,16,46,0.05)' : 'var(--card)',
                      opacity: isSelected ? 1 : 1
                    }}
                    onClick={() => !isSelected && handleAddBuilding(building.id)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                      <div style={{ flex: 1 }}>
                        <h3 style={{ margin: 0, color: 'var(--cardinal)' }}>{building.name}</h3>
                        <p className="muted" style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem' }}>
                          {building.code} • {building.type}
                        </p>
                        <p className="muted" style={{ margin: '0.5rem 0 0 0', fontSize: '0.9rem' }}>
                          {building.description.substring(0, 100)}...
                        </p>
                      </div>
                      {isSelected && (
                        <span style={{
                          padding: '0.25rem 0.75rem',
                          background: 'var(--cardinal)',
                          color: 'white',
                          borderRadius: '0.25rem',
                          fontSize: '0.875rem',
                          fontWeight: '600',
                          flexShrink: 0
                        }}>
                          Added
                        </span>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* Submit Section */}
        <div className="card" style={{ marginTop: '2rem' }}>
          {submitError && (
            <div style={{ 
              background: '#fee', 
              border: '1px solid #fcc', 
              borderRadius: '0.5rem', 
              padding: '1rem', 
              marginBottom: '1rem' 
            }}>
              <p style={{ color: '#c33', margin: 0 }}>{submitError}</p>
            </div>
          )}

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
            <div>
              <p className="muted" style={{ margin: 0 }}>
                {selectedBuildings.length === 0 
                  ? 'Add at least one building to create your tour'
                  : `Ready to create tour with ${selectedBuildings.length} stop${selectedBuildings.length !== 1 ? 's' : ''}`
                }
              </p>
            </div>
            <button
              type="submit"
              className="btn primary"
              disabled={isSubmitting || selectedBuildings.length === 0 || !tourName.trim()}
            >
              {isSubmitting ? 'Creating Tour...' : 'Create Tour'}
            </button>
          </div>
        </div>
      </form>
    </section>
  )
}
