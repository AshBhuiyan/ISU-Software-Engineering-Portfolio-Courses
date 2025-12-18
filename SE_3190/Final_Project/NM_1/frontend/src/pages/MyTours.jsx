import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function MyTours({ auth }) {
  const navigate = useNavigate()
  const [tours, setTours] = useState([])
  const [buildings, setBuildings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedTour, setSelectedTour] = useState(null)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(null)
  const [deleting, setDeleting] = useState(false)

  // Redirect if not authenticated
  if (!auth.isAuthenticated) {
    return (
      <section className="centered">
        <div className="card form-card" role="region" aria-labelledby="login-required-title">
          <h1 id="login-required-title" className="h1" style={{ color: 'var(--cardinal)' }}>Login Required</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            You must be logged in to view your tours. Please sign in to access your saved tours.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/login" className="btn primary">Go to Login</Link>
            <Link to="/" className="btn secondary">Return to Home</Link>
          </div>
        </div>
      </section>
    )
  }

  // Fetch tours and buildings
  useEffect(() => {
    if (auth.email) {
      fetchTours()
      fetchBuildings()
    }
  }, [auth.email])

  const fetchTours = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await fetch(`/api/tours?ownerEmail=${encodeURIComponent(auth.email)}`)
      if (!response.ok) {
        throw new Error('Failed to fetch tours')
      }
      const data = await response.json()
      if (data.success) {
        setTours(data.tours)
      } else {
        throw new Error(data.message || 'Failed to fetch tours')
      }
    } catch (err) {
      setError(err.message)
      console.error('Error fetching tours:', err)
    } finally {
      setLoading(false)
    }
  }

  const fetchBuildings = async () => {
    try {
      const response = await fetch('/api/buildings')
      if (response.ok) {
        const data = await response.json()
        if (data.success) {
          setBuildings(data.buildings)
        }
      }
    } catch (err) {
      console.error('Error fetching buildings:', err)
    }
  }

  // Get building details by ID
  const getBuildingById = (id) => {
    return buildings.find(b => b.id === id)
  }

  // Handle delete
  const handleDelete = async (tourId) => {
    try {
      setDeleting(true)
      const response = await fetch(`/api/tours/${tourId}`, {
        method: 'DELETE'
      })

      const data = await response.json()

      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Failed to delete tour')
      }

      // Refresh tours list
      await fetchTours()
      setShowDeleteConfirm(null)
      if (selectedTour?._id === tourId) {
        setSelectedTour(null)
      }
    } catch (err) {
      setError(err.message)
      console.error('Error deleting tour:', err)
      setShowDeleteConfirm(null)
    } finally {
      setDeleting(false)
    }
  }

  // Format date
  const formatDate = (dateString) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  return (
    <section className="campus">
      <header className="campus-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 className="display">My Tours</h1>
            <p className="muted">View and manage your saved campus tours.</p>
          </div>
          <Link to="/tours/create" className="btn primary">
            + Create New Tour
          </Link>
        </div>
      </header>

      {error && !loading && (
        <div className="card" style={{ 
          background: 'linear-gradient(135deg, #fee 0%, #fdd 100%)', 
          borderColor: '#fcc', 
          marginTop: '2rem'
        }}>
          <p style={{ color: '#c33', margin: 0, fontWeight: '600' }}>⚠️ Error</p>
          <p style={{ color: '#c33', marginTop: '0.5rem', fontSize: '0.9rem' }}>{error}</p>
          <button 
            onClick={() => { setError(null); fetchTours(); }} 
            className="btn ghost" 
            style={{ marginTop: '1rem' }}
          >
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="card" style={{ marginTop: '2rem', textAlign: 'center', padding: '3rem' }}>
          <div style={{ 
            display: 'inline-block',
            width: '40px',
            height: '40px',
            border: '4px solid var(--border)',
            borderTopColor: 'var(--cardinal)',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite',
            marginBottom: '1rem'
          }}></div>
          <p className="muted">Loading your tours...</p>
        </div>
      ) : tours.length === 0 ? (
        <div className="card" style={{ marginTop: '2rem', textAlign: 'center', padding: '3rem' }}>
          <div style={{ fontSize: '4rem', marginBottom: '1rem' }}>🗺️</div>
          <h2 className="h2" style={{ color: 'var(--cardinal)' }}>No Tours Yet</h2>
          <p className="muted" style={{ marginTop: '1rem', marginBottom: '2rem' }}>
            You haven't created any tours yet. Start planning your campus exploration!
          </p>
          <Link to="/tours/create" className="btn primary">
            Create Your First Tour
          </Link>
        </div>
      ) : (
        <div style={{ marginTop: '2rem', display: 'grid', gap: '2rem' }}>
          {/* Tours List */}
          <div className="card">
            <h2 className="h2">Your Saved Tours ({tours.length})</h2>
            <div style={{ marginTop: '1.5rem', display: 'grid', gap: '1rem' }}>
              {tours.map(tour => (
                <div
                  key={tour._id}
                  className="card"
                  style={{
                    padding: '1.5rem',
                    cursor: 'pointer',
                    border: selectedTour?._id === tour._id ? '2px solid var(--cardinal)' : '1px solid var(--border)',
                    background: selectedTour?._id === tour._id ? 'rgba(200,16,46,0.05)' : 'var(--card)'
                  }}
                  onClick={() => setSelectedTour(tour)}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '1rem' }}>
                    <div style={{ flex: 1 }}>
                      <h3 style={{ margin: 0, color: 'var(--cardinal)', fontSize: '1.25rem' }}>
                        {tour.name}
                      </h3>
                      {tour.description && (
                        <p className="muted" style={{ marginTop: '0.5rem', fontSize: '0.9rem' }}>
                          {tour.description}
                        </p>
                      )}
                      <div style={{ marginTop: '0.75rem', display: 'flex', gap: '1rem', flexWrap: 'wrap', fontSize: '0.875rem' }}>
                        <span className="muted">
                          📍 {tour.buildingIds?.length || 0} stop{tour.buildingIds?.length !== 1 ? 's' : ''}
                        </span>
                        <span className="muted">
                          📅 Created {formatDate(tour.createdAt)}
                        </span>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', flexShrink: 0 }}>
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          setSelectedTour(tour)
                        }}
                        className="btn secondary"
                        style={{ fontSize: '0.875rem', padding: '0.5rem 1rem' }}
                      >
                        View Details
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          setShowDeleteConfirm(tour._id)
                        }}
                        className="btn ghost"
                        style={{ fontSize: '0.875rem', padding: '0.5rem 1rem', color: '#c33' }}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Tour Details Panel */}
          {selectedTour && (
            <div className="card" style={{ background: 'linear-gradient(135deg, rgba(200,16,46,0.05) 0%, rgba(241,190,72,0.05) 100%)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1rem' }}>
                <h2 className="h2" style={{ margin: 0 }}>Tour Details</h2>
                <button
                  className="btn ghost"
                  onClick={() => setSelectedTour(null)}
                  style={{ padding: '0.5rem 1rem' }}
                >
                  ✕ Close
                </button>
              </div>

              <h3 className="h3" style={{ color: 'var(--cardinal)' }}>{selectedTour.name}</h3>
              
              {selectedTour.description && (
                <p className="muted" style={{ marginTop: '0.5rem' }}>
                  {selectedTour.description}
                </p>
              )}

              <div style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
                <h4 className="h4" style={{ margin: 0, flex: 1 }}>
                  Tour Stops ({selectedTour.buildingIds?.length || 0})
                </h4>
                <button
                  onClick={() => {
                    navigate('/campus', { state: { activeTour: selectedTour } })
                  }}
                  className="btn primary"
                  style={{ fontSize: '0.875rem', padding: '0.5rem 1rem' }}
                >
                  🗺️ View on Map
                </button>
              </div>

              <div style={{ display: 'grid', gap: '1rem' }}>
                {selectedTour.buildingIds?.map((buildingId, index) => {
                  const building = getBuildingById(buildingId)
                  if (!building) {
                    return (
                      <div key={index} className="card" style={{ padding: '1rem', background: '#fee' }}>
                        <p style={{ margin: 0, color: '#c33' }}>
                          Building "{buildingId}" not found
                        </p>
                      </div>
                    )
                  }

                  return (
                    <div 
                      key={buildingId}
                      className="card"
                      style={{ padding: '1rem' }}
                    >
                      <div style={{ display: 'flex', alignItems: 'start', gap: '1rem' }}>
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
                          <h4 style={{ margin: 0, color: 'var(--cardinal)' }}>{building.name}</h4>
                          <p className="muted" style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem' }}>
                            {building.code} • {building.type}
                          </p>
                          <p className="muted" style={{ margin: '0.5rem 0 0 0', fontSize: '0.9rem' }}>
                            {building.description}
                          </p>
                          <button
                            onClick={() => {
                              navigate('/campus', { state: { activeTour: selectedTour, selectedBuildingId: building.id } })
                            }}
                            className="btn ghost" 
                            style={{ marginTop: '0.5rem', fontSize: '0.875rem', padding: '0.25rem 0.75rem' }}
                          >
                            View on Map →
                          </button>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>

              <div style={{ marginTop: '1.5rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border)' }}>
                <p className="muted" style={{ fontSize: '0.875rem' }}>
                  Created: {formatDate(selectedTour.createdAt)}
                  {selectedTour.updatedAt !== selectedTour.createdAt && (
                    <> • Updated: {formatDate(selectedTour.updatedAt)}</>
                  )}
                </p>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card" style={{ maxWidth: '500px', width: '100%' }}>
            <h2 className="h2" style={{ color: '#c33' }}>Confirm Delete</h2>
            <p className="muted" style={{ marginTop: '1rem' }}>
              Are you sure you want to delete <strong>{tours.find(t => t._id === showDeleteConfirm)?.name}</strong>?
              This action cannot be undone.
            </p>
            <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowDeleteConfirm(null)}
                className="btn secondary"
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(showDeleteConfirm)}
                className="btn primary"
                style={{ background: '#c33', borderColor: '#c33' }}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Delete Tour'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
