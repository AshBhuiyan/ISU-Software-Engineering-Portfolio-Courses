import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AdminBuildings({ auth }) {
  const navigate = useNavigate()
  const [buildings, setBuildings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [editingBuilding, setEditingBuilding] = useState(null)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(null)
  const [formData, setFormData] = useState({
    id: '',
    name: '',
    code: '',
    type: 'Academic',
    departments: '',
    description: '',
    hours: '',
    capacity: '',
    yearBuilt: '',
    floors: ''
  })
  const [formError, setFormError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Redirect if not authenticated or not admin
  if (!auth.isAuthenticated || auth.role !== 'admin') {
    return (
      <section className="centered">
        <div className="card form-card" role="region" aria-labelledby="unauthorized-title">
          <h1 id="unauthorized-title" className="h1" style={{ color: 'var(--cardinal)' }}>Not Authorized</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            This page is restricted to administrators only.
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
      if (data.success) {
        setBuildings(data.buildings)
      } else {
        throw new Error(data.message || 'Failed to fetch buildings')
      }
    } catch (err) {
      setError(err.message)
      console.error('Error fetching buildings:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    setFormError(null)
  }

  const resetForm = () => {
    setFormData({
      id: '',
      name: '',
      code: '',
      type: 'Academic',
      departments: '',
      description: '',
      hours: '',
      capacity: '',
      yearBuilt: '',
      floors: ''
    })
    setEditingBuilding(null)
    setFormError(null)
  }

  const handleEdit = (building) => {
    setEditingBuilding(building)
    setFormData({
      id: building.id,
      name: building.name,
      code: building.code,
      type: building.type,
      departments: building.departments.join(', '),
      description: building.description,
      hours: building.hours,
      capacity: building.capacity,
      yearBuilt: building.yearBuilt,
      floors: building.floors.toString()
    })
    // Scroll to form
    document.getElementById('building-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setIsSubmitting(true)
    setFormError(null)

    try {
      // Parse departments from comma-separated string
      const departments = formData.departments
        .split(',')
        .map(d => d.trim())
        .filter(d => d.length > 0)

      const buildingData = {
        id: formData.id.trim().toLowerCase(),
        name: formData.name.trim(),
        code: formData.code.trim().toUpperCase(),
        type: formData.type,
        departments,
        description: formData.description.trim(),
        hours: formData.hours.trim(),
        capacity: formData.capacity.trim(),
        yearBuilt: formData.yearBuilt.trim(),
        floors: parseInt(formData.floors)
      }

      let response
      if (editingBuilding) {
        // Update existing building
        response = await fetch(`/api/buildings/${editingBuilding.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(buildingData)
        })
      } else {
        // Create new building
        response = await fetch('/api/buildings', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(buildingData)
        })
      }

      const data = await response.json()

      if (!response.ok || !data.success) {
        throw new Error(data.message || `Failed to ${editingBuilding ? 'update' : 'create'} building`)
      }

      // Refresh buildings list
      await fetchBuildings()
      resetForm()
    } catch (err) {
      setFormError(err.message)
      console.error('Error saving building:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async (buildingId) => {
    try {
      const response = await fetch(`/api/buildings/${buildingId}`, {
        method: 'DELETE'
      })

      const data = await response.json()

      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Failed to delete building')
      }

      // Refresh buildings list
      await fetchBuildings()
      setShowDeleteConfirm(null)
    } catch (err) {
      setError(err.message)
      console.error('Error deleting building:', err)
      setShowDeleteConfirm(null)
    }
  }

  const buildingTypes = ['Academic', 'Administration', 'Student Life', 'Athletics', 'Residence', 'Landmark']

  return (
    <section className="campus">
      <header className="campus-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 className="display">Building Management</h1>
            <p className="muted">Manage campus buildings: create, edit, and delete building information.</p>
          </div>
          <Link to="/admin" className="btn secondary">
            ← Back to Dashboard
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
            onClick={() => { setError(null); fetchBuildings(); }} 
            className="btn ghost" 
            style={{ marginTop: '1rem' }}
          >
            Retry
          </button>
        </div>
      )}

      {/* Building Form */}
      <div id="building-form" className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">
          {editingBuilding ? `Edit Building: ${editingBuilding.name}` : 'Create New Building'}
        </h2>
        
        {formError && (
          <div style={{ 
            background: '#fee', 
            border: '1px solid #fcc', 
            borderRadius: '0.5rem', 
            padding: '1rem', 
            marginBottom: '1rem' 
          }}>
            <p style={{ color: '#c33', margin: 0 }}>{formError}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
            <div>
              <label htmlFor="id">Building ID *</label>
              <input
                id="id"
                name="id"
                type="text"
                value={formData.id}
                onChange={handleInputChange}
                placeholder="e.g., library"
                required
                disabled={!!editingBuilding}
                style={{ fontFamily: 'monospace' }}
              />
              <small className="muted">Unique identifier (lowercase, no spaces)</small>
            </div>

            <div>
              <label htmlFor="name">Building Name *</label>
              <input
                id="name"
                name="name"
                type="text"
                value={formData.name}
                onChange={handleInputChange}
                placeholder="e.g., Parks Library"
                required
              />
            </div>

            <div>
              <label htmlFor="code">Building Code *</label>
              <input
                id="code"
                name="code"
                type="text"
                value={formData.code}
                onChange={handleInputChange}
                placeholder="e.g., LIB"
                required
                maxLength={10}
                style={{ fontFamily: 'monospace', textTransform: 'uppercase' }}
              />
            </div>

            <div>
              <label htmlFor="type">Building Type *</label>
              <select
                id="type"
                name="type"
                value={formData.type}
                onChange={handleInputChange}
                required
              >
                {buildingTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="yearBuilt">Year Built *</label>
              <input
                id="yearBuilt"
                name="yearBuilt"
                type="text"
                value={formData.yearBuilt}
                onChange={handleInputChange}
                placeholder="e.g., 1925"
                required
              />
            </div>

            <div>
              <label htmlFor="floors">Number of Floors *</label>
              <input
                id="floors"
                name="floors"
                type="number"
                value={formData.floors}
                onChange={handleInputChange}
                placeholder="e.g., 4"
                required
                min="1"
              />
            </div>
          </div>

          <div style={{ marginTop: '1rem' }}>
            <label htmlFor="description">Description *</label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="Detailed description of the building..."
              required
              rows="3"
            />
          </div>

          <div style={{ marginTop: '1rem' }}>
            <label htmlFor="hours">Operating Hours *</label>
            <input
              id="hours"
              name="hours"
              type="text"
              value={formData.hours}
              onChange={handleInputChange}
              placeholder="e.g., Mon-Fri: 8:00am-5:00pm"
              required
            />
          </div>

          <div style={{ marginTop: '1rem' }}>
            <label htmlFor="capacity">Capacity *</label>
            <input
              id="capacity"
              name="capacity"
              type="text"
              value={formData.capacity}
              onChange={handleInputChange}
              placeholder="e.g., 2,500 students"
              required
            />
          </div>

          <div style={{ marginTop: '1rem' }}>
            <label htmlFor="departments">Departments</label>
            <input
              id="departments"
              name="departments"
              type="text"
              value={formData.departments}
              onChange={handleInputChange}
              placeholder="Comma-separated: Library Services, Research Support"
            />
            <small className="muted">Separate multiple departments with commas</small>
          </div>

          <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem', flexWrap: 'wrap' }}>
            <button 
              type="submit" 
              className="btn primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : (editingBuilding ? 'Update Building' : 'Create Building')}
            </button>
            {editingBuilding && (
              <button 
                type="button" 
                className="btn secondary"
                onClick={resetForm}
                disabled={isSubmitting}
              >
                Cancel Edit
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Buildings List */}
      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">All Buildings ({buildings.length})</h2>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
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
            <p className="muted">Loading buildings...</p>
          </div>
        ) : buildings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <p className="muted" style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>
              No buildings found. Create your first building above!
            </p>
          </div>
        ) : (
          <div style={{ marginTop: '1.5rem' }}>
            <div style={{ display: 'grid', gap: '1rem' }}>
              {buildings.map(building => (
                <div 
                  key={building._id || building.id} 
                  className="card"
                  style={{ 
                    padding: '1.5rem',
                    border: editingBuilding?.id === building.id ? '2px solid var(--cardinal)' : '1px solid var(--border)'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '1rem' }}>
                    <div style={{ flex: 1 }}>
                      <h3 style={{ margin: 0, color: 'var(--cardinal)', fontSize: '1.25rem' }}>
                        {building.name}
                      </h3>
                      <p className="muted" style={{ marginTop: '0.25rem', fontSize: '0.9rem' }}>
                        <code>{building.code}</code> • {building.type} • {building.floors} floor{building.floors !== 1 ? 's' : ''}
                      </p>
                      <p className="muted" style={{ marginTop: '0.75rem', fontSize: '0.9rem' }}>
                        {building.description}
                      </p>
                      <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        {building.departments && building.departments.length > 0 && building.departments.map((dept, idx) => (
                          <span 
                            key={idx}
                            style={{
                              padding: '0.25rem 0.5rem',
                              background: 'var(--hover-bg)',
                              borderRadius: '0.25rem',
                              fontSize: '0.75rem',
                              color: 'var(--text)'
                            }}
                          >
                            {dept}
                          </span>
                        ))}
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        onClick={() => handleEdit(building)}
                        className="btn secondary"
                        style={{ fontSize: '0.875rem', padding: '0.5rem 1rem' }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => setShowDeleteConfirm(building.id)}
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
        )}
      </div>

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
              Are you sure you want to delete <strong>{buildings.find(b => b.id === showDeleteConfirm)?.name}</strong>?
              This action cannot be undone.
            </p>
            <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowDeleteConfirm(null)}
                className="btn secondary"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(showDeleteConfirm)}
                className="btn primary"
                style={{ background: '#c33', borderColor: '#c33' }}
              >
                Delete Building
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
