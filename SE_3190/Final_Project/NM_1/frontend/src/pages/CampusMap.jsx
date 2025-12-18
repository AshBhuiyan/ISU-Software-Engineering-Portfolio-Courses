import React, { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import grassTexture from '../../assets/grass-texture-background.jpg'

// Building positions for the map visualization (kept for visual layout)
const buildingPositions = {
  'library': { x: 50, y: 50, w: 120, h: 50 },
  'beardshear': { x: 450, y: 40, w: 110, h: 40 },
  'memorial': { x: 280, y: 200, w: 140, h: 55 },
  'campanile': { x: 80, y: 280, w: 80, h: 50 },
  'hilton': { x: 520, y: 250, w: 120, h: 60 },
  'carver': { x: 550, y: 120, w: 100, h: 45 },
  'friley': { x: 40, y: 400, w: 80, h: 50 },
  'coover': { x: 300, y: 100, w: 100, h: 45 },
  'sukup': { x: 400, y: 350, w: 90, h: 50 },
  'state-gym': { x: 200, y: 380, w: 110, h: 50 },
}

const buildingTypes = ['All', 'Academic', 'Administration', 'Student Life', 'Athletics', 'Residence', 'Landmark']

export default function CampusMap({ auth, activeTour: propActiveTour }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [buildings, setBuildings] = useState([])
  
  // Get active tour from props or location state
  const activeTour = propActiveTour || location.state?.activeTour
  const selectedBuildingIdFromState = location.state?.selectedBuildingId
  const [selectedBuilding, setSelectedBuilding] = useState(null)
  const [hoveredBuilding, setHoveredBuilding] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedType, setSelectedType] = useState('All')
  const [isAnimating, setIsAnimating] = useState(false)
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [highlightedBuildingId, setHighlightedBuildingId] = useState(null)
  const [isEditMode, setIsEditMode] = useState(false)
  const [draggingBuildingId, setDraggingBuildingId] = useState(null)
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 })
  const [notification, setNotification] = useState(null)

  // Fetch buildings from backend
  useEffect(() => {
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
          // Merge backend data with visual positions
          const buildingsWithPositions = data.buildings.map(building => {
            const position = buildingPositions[building.id] || { x: 100, y: 100, w: 80, h: 40 }
            
            // Use coordinates from DB if available, otherwise use hardcoded positions
            let x, y
            if (building.coordinates && building.coordinates.x !== null && building.coordinates.y !== null) {
              // Convert percentage to SVG coordinates (700x500 viewBox)
              x = (building.coordinates.x / 100) * 700 - position.w / 2
              y = (building.coordinates.y / 100) * 500 - position.h / 2
            } else {
              x = position.x
              y = position.y
            }
            
            return {
              ...building,
              desc: building.description, // Map description field
              x: x,
              y: y,
              w: position.w,
              h: position.h,
            }
          })
          setBuildings(buildingsWithPositions)
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

    fetchBuildings()
  }, [])

  // Handle selectedBuildingId from navigation state
  useEffect(() => {
    if (selectedBuildingIdFromState && buildings.length > 0) {
      const building = buildings.find(b => b.id === selectedBuildingIdFromState)
      if (building) {
        setSelectedBuilding(building)
        setHighlightedBuildingId(building.id)
        // Remove highlight after animation completes
        setTimeout(() => {
          setHighlightedBuildingId(null)
        }, 2000)
      }
    }
  }, [selectedBuildingIdFromState, buildings])

  const filteredBuildings = buildings.filter(building => {
    const matchesSearch = building.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (building.departments && building.departments.some(dept => dept.toLowerCase().includes(searchTerm.toLowerCase())))
    const matchesType = selectedType === 'All' || selectedType === building.type
    return matchesSearch && matchesType
  })

  // Get tour building positions for visualization
  const getTourBuildingPositions = () => {
    if (!activeTour || !activeTour.buildingIds) return []
    
    return activeTour.buildingIds.map((buildingId, index) => {
      const building = buildings.find(b => b.id === buildingId)
      if (!building) return null
      
      // Use coordinates from DB if available, otherwise use buildingPositions
      let x, y
      if (building.coordinates && building.coordinates.x !== null && building.coordinates.y !== null) {
        // Convert percentage to SVG coordinates (700x500 viewBox)
        x = (building.coordinates.x / 100) * 700
        y = (building.coordinates.y / 100) * 500
      } else {
        // Use existing buildingPositions (center of building)
        x = building.x + building.w / 2
        y = building.y + building.h / 2
      }
      
      return { buildingId, x, y, index: index + 1, building }
    }).filter(Boolean)
  }

  const tourPositions = getTourBuildingPositions()

  // Get building center position for tour badge
  const getBuildingCenter = (building) => {
    if (building.coordinates && building.coordinates.x !== null && building.coordinates.y !== null) {
      // Convert percentage to SVG coordinates
      return {
        x: (building.coordinates.x / 100) * 700,
        y: (building.coordinates.y / 100) * 500
      }
    }
    // Use building's SVG position (center)
    return {
      x: building.x + building.w / 2,
      y: building.y + building.h / 2
    }
  }

  const handleBuildingClick = (building) => {
    if (isTransitioning) return 
    
    setIsTransitioning(true)
    setIsAnimating(true)
    
    
    if (selectedBuilding?.id === building.id) {
      setSelectedBuilding(null)
    } else {
      setSelectedBuilding(building)
    }
    
    
    setHoveredBuilding(null)
    
    setTimeout(() => {
      setIsAnimating(false)
      setIsTransitioning(false)
    }, 300)
  }

  const handleBuildingHover = (building) => {
    if (isTransitioning) return 
    setHoveredBuilding(building)
  }

  const handleBuildingLeave = () => {
    if (isTransitioning) return
    setHoveredBuilding(null)
  }

  const handleKeyDown = (e, building) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      handleBuildingClick(building)
    }
  }

  // Global mouse move handler for dragging
  useEffect(() => {
    if (!draggingBuildingId || !isEditMode) return

    const handleGlobalMouseMove = (e) => {
      const svg = document.querySelector('.interactive-map')
      if (!svg) return

      const point = svg.createSVGPoint()
      point.x = e.clientX
      point.y = e.clientY
      const svgPoint = point.matrixTransform(svg.getScreenCTM().inverse())

      // Update building position in real-time during drag
      setBuildings(prevBuildings =>
        prevBuildings.map(b => {
          if (b.id === draggingBuildingId) {
            // Calculate new center position
            const newX = svgPoint.x - dragOffset.x
            const newY = svgPoint.y - dragOffset.y

            // Convert SVG coordinates to percentages
            const xPercent = (newX / 700) * 100
            const yPercent = (newY / 500) * 100

            // Clamp to map bounds (with some padding)
            const clampedX = Math.max(5, Math.min(95, xPercent))
            const clampedY = Math.max(5, Math.min(95, yPercent))

            return {
              ...b,
              coordinates: {
                x: clampedX,
                y: clampedY
              }
            }
          }
          return b
        })
      )
    }

    const handleGlobalMouseUp = async (e) => {
      if (!draggingBuildingId) return

      const svg = document.querySelector('.interactive-map')
      if (!svg) return

      const building = buildings.find(b => b.id === draggingBuildingId)
      if (!building) {
        setDraggingBuildingId(null)
        setDragOffset({ x: 0, y: 0 })
        return
      }

      const point = svg.createSVGPoint()
      point.x = e.clientX
      point.y = e.clientY
      const svgPoint = point.matrixTransform(svg.getScreenCTM().inverse())

      // Calculate final position
      const newX = svgPoint.x - dragOffset.x
      const newY = svgPoint.y - dragOffset.y

      // Convert SVG coordinates (0-700, 0-500) to percentages (0-100)
      const xPercent = (newX / 700) * 100
      const yPercent = (newY / 500) * 100

      // Clamp to map bounds (with padding to keep buildings visible)
      const clampedX = Math.max(5, Math.min(95, xPercent))
      const clampedY = Math.max(5, Math.min(95, yPercent))

      const newCoordinates = {
        x: clampedX,
        y: clampedY
      }

      // Save to backend
      try {
        const response = await fetch(`/api/buildings/${building.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ coordinates: newCoordinates })
        })

        const data = await response.json()

        if (!response.ok || !data.success) {
          throw new Error(data.message || 'Failed to update building position')
        }

        // Update local state with saved building
        setBuildings(prevBuildings =>
          prevBuildings.map(b =>
            b.id === building.id
              ? { ...b, coordinates: newCoordinates }
              : b
          )
        )

        // Show success notification
        setNotification({
          type: 'success',
          message: `${building.name} position updated successfully`
        })

        // Clear notification after 3 seconds
        setTimeout(() => setNotification(null), 3000)
      } catch (err) {
        console.error('Error updating building position:', err)
        setNotification({
          type: 'error',
          message: `Failed to update ${building.name} position: ${err.message}`
        })
        setTimeout(() => setNotification(null), 3000)

        // Revert to previous position on error
        const response = await fetch('/api/buildings')
        if (response.ok) {
          const data = await response.json()
          if (data.success && data.buildings) {
            const buildingsWithPositions = data.buildings.map(b => {
              const position = buildingPositions[b.id] || { x: 100, y: 100, w: 80, h: 40 }
              let x, y
              if (b.coordinates && b.coordinates.x !== null && b.coordinates.y !== null) {
                x = (b.coordinates.x / 100) * 700 - position.w / 2
                y = (b.coordinates.y / 100) * 500 - position.h / 2
              } else {
                x = position.x
                y = position.y
              }
              return {
                ...b,
                desc: b.description,
                x: x,
                y: y,
                w: position.w,
                h: position.h,
              }
            })
            setBuildings(buildingsWithPositions)
          }
        }
      } finally {
        setDraggingBuildingId(null)
        setDragOffset({ x: 0, y: 0 })
      }
    }

    document.addEventListener('mousemove', handleGlobalMouseMove)
    document.addEventListener('mouseup', handleGlobalMouseUp)

    return () => {
      document.removeEventListener('mousemove', handleGlobalMouseMove)
      document.removeEventListener('mouseup', handleGlobalMouseUp)
    }
  }, [draggingBuildingId, dragOffset, isEditMode, buildings])

  // Drag and drop handlers for admin edit mode
  const handleDragStart = (e, building) => {
    if (!isEditMode || auth?.role !== 'admin') return

    e.preventDefault()
    e.stopPropagation()

    const svg = e.currentTarget.closest('svg')
    if (!svg) return

    const point = svg.createSVGPoint()
    point.x = e.clientX
    point.y = e.clientY
    const svgPoint = point.matrixTransform(svg.getScreenCTM().inverse())

    // Calculate building center position
    let buildingCenterX, buildingCenterY
    if (building.coordinates && building.coordinates.x !== null && building.coordinates.y !== null) {
      buildingCenterX = (building.coordinates.x / 100) * 700
      buildingCenterY = (building.coordinates.y / 100) * 500
    } else {
      buildingCenterX = building.x + building.w / 2
      buildingCenterY = building.y + building.h / 2
    }

    setDragOffset({
      x: svgPoint.x - buildingCenterX,
      y: svgPoint.y - buildingCenterY
    })
    setDraggingBuildingId(building.id)
  }

  const handleDragEnd = async (e, building) => {
    if (!isEditMode || !draggingBuildingId || auth?.role !== 'admin') return
    
    e.preventDefault()
    e.stopPropagation()
    
    const svg = e.currentTarget.closest('svg')
    if (!svg) return
    
    const point = svg.createSVGPoint()
    point.x = e.clientX
    point.y = e.clientY
    const svgPoint = point.matrixTransform(svg.getScreenCTM().inverse())
    
    // Calculate final position
    const newX = svgPoint.x - dragOffset.x
    const newY = svgPoint.y - dragOffset.y
    
    // Convert SVG coordinates (0-700, 0-500) to percentages (0-100)
    const xPercent = (newX / 700) * 100
    const yPercent = (newY / 500) * 100
    
    // Clamp to map bounds (with padding to keep buildings visible)
    const clampedX = Math.max(5, Math.min(95, xPercent))
    const clampedY = Math.max(5, Math.min(95, yPercent))
    
    const newCoordinates = {
      x: clampedX,
      y: clampedY
    }
    
    // Save to backend
    try {
      const response = await fetch(`/api/buildings/${building.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ coordinates: newCoordinates })
      })
      
      const data = await response.json()
      
      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Failed to update building position')
      }
      
      // Update local state with saved building
      setBuildings(prevBuildings =>
        prevBuildings.map(b =>
          b.id === building.id
            ? { ...b, coordinates: newCoordinates }
            : b
        )
      )
      
      // Show success notification
      setNotification({
        type: 'success',
        message: `${building.name} position updated successfully`
      })
      
      // Clear notification after 3 seconds
      setTimeout(() => setNotification(null), 3000)
    } catch (err) {
      console.error('Error updating building position:', err)
      setNotification({
        type: 'error',
        message: `Failed to update ${building.name} position: ${err.message}`
      })
      setTimeout(() => setNotification(null), 3000)
      
      // Revert to previous position on error
      const response = await fetch('/api/buildings')
      if (response.ok) {
        const data = await response.json()
        if (data.success && data.buildings) {
          const buildingsWithPositions = data.buildings.map(b => {
            const position = buildingPositions[b.id] || { x: 100, y: 100, w: 80, h: 40 }
            return {
              ...b,
              desc: b.description,
              x: position.x,
              y: position.y,
              w: position.w,
              h: position.h,
            }
          })
          setBuildings(buildingsWithPositions)
        }
      }
    } finally {
      setDraggingBuildingId(null)
      setDragOffset({ x: 0, y: 0 })
    }
  }

  
  useEffect(() => {
    if (selectedBuilding && !filteredBuildings.find(b => b.id === selectedBuilding.id)) {
      setSelectedBuilding(null)
    }
  }, [filteredBuildings, selectedBuilding])

  const currentBuilding = selectedBuilding || hoveredBuilding

  return (
    <section className="campus">
      <header className="campus-header">
        <div style={{ maxWidth: '800px', margin: '0 auto' }}>
          <h1 className="display" style={{ marginBottom: '0.75rem' }}>
            {activeTour ? `Tour Route: ${activeTour.name}` : 'Campus Map'}
          </h1>
          <p className="muted" style={{ fontSize: '1.1rem', marginBottom: '2.5rem' }}>
            {activeTour 
              ? `Visual tour route with ${activeTour.buildingIds?.length || 0} stops. Red lines show the tour path, numbered badges indicate stop order.`
              : "Discover Iowa State University's campus with our interactive map. Search, filter, and explore buildings to find exactly what you're looking for."
            }
          </p>
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
            {activeTour && (
              <button
                onClick={() => navigate('/tours', { replace: true })}
                className="btn secondary"
              >
                ← Back to My Tours
              </button>
            )}
            {auth?.role === 'admin' && (
              <button
                onClick={() => setIsEditMode(!isEditMode)}
                className={isEditMode ? "btn primary" : "btn secondary"}
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '0.5rem',
                  fontSize: '0.9rem'
                }}
              >
                {isEditMode ? '✓' : '✎'} {isEditMode ? 'Exit Edit Mode' : 'Edit Map Positions'}
              </button>
            )}
          </div>
        </div>
        
        {loading && (
          <div style={{ textAlign: 'center', marginTop: '2rem', padding: '3rem' }}>
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
            <p className="muted" style={{ fontSize: '0.95rem' }}>Loading building data...</p>
          </div>
        )}

        {error && (
          <div className="card" style={{ 
            background: 'linear-gradient(135deg, #fee 0%, #fdd 100%)', 
            borderColor: '#fcc', 
            marginTop: '2rem',
            maxWidth: '600px',
            marginLeft: 'auto',
            marginRight: 'auto'
          }}>
            <p style={{ color: '#c33', margin: 0, fontWeight: '600' }}>⚠️ Error loading buildings</p>
            <p style={{ color: '#c33', marginTop: '0.5rem', fontSize: '0.9rem' }}>
              {error}. Make sure the backend server is running on http://localhost:3000
            </p>
          </div>
        )}

        {/* Enhanced Search and Filter Controls */}
        {!loading && !error && (
          <div className="map-controls-enhanced">
            <div className="search-container-enhanced">
              <svg 
                width="20" 
                height="20" 
                viewBox="0 0 24 24" 
                fill="none" 
                stroke="currentColor" 
                strokeWidth="2"
                style={{ 
                  position: 'absolute',
                  right: '0.9rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-muted)',
                  pointerEvents: 'none'
                }}
              >
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
              </svg>
              <input
                type="text"
                placeholder="Search by name, code, or department..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="search-input-enhanced"
              />
              {searchTerm && (
                <button
                  onClick={() => setSearchTerm('')}
                  style={{
                    position: 'absolute',
                    right: '3.5rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent',
                    border: 'none',
                    cursor: 'pointer',
                    color: 'var(--text-muted)',
                    padding: '0.25rem',
                    display: 'flex',
                    alignItems: 'center',
                    borderRadius: '50%',
                    transition: 'all 0.2s ease'
                  }}
                  onMouseEnter={(e) => e.target.style.background = 'var(--hover-bg)'}
                  onMouseLeave={(e) => e.target.style.background = 'transparent'}
                  aria-label="Clear search"
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              )}
            </div>
            
            <div className="filter-container-enhanced">
              <select
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
                className="filter-select-enhanced"
              >
                {buildingTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>
          </div>
        )}
      </header>

      {!loading && !error && (
        <div className="map-container">
          <div className="map-wrapper">
            <svg 
              viewBox="0 0 700 500" 
              className="interactive-map" 
              role="img" 
              aria-label="Interactive Iowa State University campus map"
              preserveAspectRatio="xMidYMid meet"
            >
            <defs>
              {/* ISU Cardinal and Gold gradient border */}
              <linearGradient id="mapBorderGradient" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="#c8102e" stopOpacity="0.8" />
                <stop offset="50%" stopColor="#f1be48" stopOpacity="0.6" />
                <stop offset="100%" stopColor="#c8102e" stopOpacity="0.8" />
              </linearGradient>
              
              {/* Professional subtle grid pattern */}
              <pattern id="subtleGrid" x="0" y="0" width="40" height="40" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(0,0,0,0.02)" strokeWidth="0.5"/>
              </pattern>
              
              {/* Grass texture pattern */}
              <pattern id="grassTexture" x="0" y="0" width="1" height="1" patternUnits="objectBoundingBox" preserveAspectRatio="none">
                <image href={grassTexture} width="100%" height="100%" preserveAspectRatio="xMidYMid slice"/>
              </pattern>
              
              {/* Building shadow filter */}
              <filter id="buildingShadow" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur in="SourceAlpha" stdDeviation="2"/>
                <feOffset dx="0" dy="2" result="offsetblur"/>
                <feComponentTransfer>
                  <feFuncA type="linear" slope="0.15"/>
                </feComponentTransfer>
                <feMerge>
                  <feMergeNode/>
                  <feMergeNode in="SourceGraphic"/>
                </feMerge>
              </filter>
              
              {/* Hover glow filter */}
              <filter id="hoverGlow" x="-100%" y="-100%" width="300%" height="300%">
                <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
                <feOffset in="coloredBlur" dx="0" dy="0"/>
                <feMerge> 
                  <feMergeNode in="coloredBlur"/>
                  <feMergeNode in="SourceGraphic"/>
                </feMerge>
              </filter>
              
              {/* Selection highlight */}
              <filter id="selectionGlow" x="-100%" y="-100%" width="300%" height="300%">
                <feGaussianBlur stdDeviation="5" result="coloredBlur"/>
                <feOffset in="coloredBlur" dx="0" dy="0"/>
                <feMerge> 
                  <feMergeNode in="coloredBlur"/>
                  <feMergeNode in="SourceGraphic"/>
                </feMerge>
              </filter>
              
              {/* Building gradient for depth */}
              <linearGradient id="buildingGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="rgba(255,255,255,0.3)" stopOpacity="0.4"/>
                <stop offset="100%" stopColor="rgba(0,0,0,0.1)" stopOpacity="0.2"/>
              </linearGradient>
            </defs>
            
            {/* ISU gradient border */}
            <rect x="0" y="0" width="700" height="500" rx="16" fill="url(#mapBorderGradient)" />
            
            {/* Grass texture map background */}
            <rect x="8" y="8" width="684" height="484" rx="12" fill="url(#grassTexture)" />
            
            {/* Subtle grid overlay */}
            <rect x="8" y="8" width="684" height="484" fill="url(#subtleGrid)" opacity="0.3" />
            
            {/* Pathways - subtle lines (disabled) */}

            {/* Tour Route Lines - Visual Tour Routing Feature */}
            {activeTour && tourPositions.length > 1 && (
              <g id="tour-route">
                {tourPositions.map((pos, idx) => {
                  if (idx === tourPositions.length - 1) return null
                  const nextPos = tourPositions[idx + 1]
                  return (
                    <line
                      key={`route-${idx}`}
                      x1={pos.x}
                      y1={pos.y}
                      x2={nextPos.x}
                      y2={nextPos.y}
                      stroke="#c8102e"
                      strokeWidth="3"
                      strokeDasharray="5,5"
                      opacity="0.8"
                      markerEnd="url(#arrowhead)"
                    />
                  )
                })}
                {/* Arrow marker definition */}
                <defs>
                  <marker
                    id="arrowhead"
                    markerWidth="10"
                    markerHeight="10"
                    refX="9"
                    refY="3"
                    orient="auto"
                  >
                    <polygon
                      points="0 0, 10 3, 0 6"
                      fill="#c8102e"
                    />
                  </marker>
                </defs>
              </g>
            )}

            {/* Buildings */}
            {filteredBuildings.map(building => {
              const isSelected = selectedBuilding?.id === building.id
              const isHovered = hoveredBuilding?.id === building.id && !isSelected
              const isHighlighted = isSelected || isHovered
              const isPulseHighlighted = highlightedBuildingId === building.id
              const isDragging = draggingBuildingId === building.id
              const isDraggable = isEditMode && auth?.role === 'admin'
              
              // Calculate building position - use coordinates from DB if available
              let buildingX = building.x
              let buildingY = building.y
              if (building.coordinates && building.coordinates.x !== null && building.coordinates.y !== null) {
                // Convert percentage to SVG coordinates, then adjust for building dimensions
                buildingX = (building.coordinates.x / 100) * 700 - building.w / 2
                buildingY = (building.coordinates.y / 100) * 500 - building.h / 2
              }
              
              return (
                <g
                  key={building.id}
                  className={`building-group ${isHighlighted ? 'highlighted' : ''} ${isSelected ? 'selected' : ''} ${isHovered ? 'hovered' : ''} ${isPulseHighlighted ? 'highlight-pulse' : ''} ${isDragging ? 'dragging' : ''} ${isDraggable ? 'draggable' : ''}`}
                  onMouseEnter={() => !isEditMode && handleBuildingHover(building)}
                  onMouseLeave={() => !isEditMode && handleBuildingLeave()}
                  onClick={(e) => {
                    if (!isEditMode) {
                      handleBuildingClick(building)
                    }
                  }}
                  onMouseDown={(e) => isDraggable && handleDragStart(e, building)}
                  tabIndex="0"
                  role="button"
                  aria-label={`${building.name} - ${building.type}. ${isSelected ? 'Currently selected' : isEditMode ? 'Drag to reposition' : 'Click to select'}`}
                  aria-pressed={isSelected}
                  onFocus={() => !isEditMode && handleBuildingHover(building)}
                  onBlur={() => !isEditMode && handleBuildingLeave()}
                  onKeyDown={(e) => !isEditMode && handleKeyDown(e, building)}
                  style={{
                    cursor: isDraggable ? (isDragging ? 'grabbing' : 'grab') : 'pointer'
                  }}
                >
                  {/* Professional building shadow */}
                  <rect
                    x={buildingX + 2}
                    y={buildingY + 4}
                    width={building.w}
                    height={building.h}
                    rx="6"
                    fill="rgba(0,0,0,0.12)"
                    className="building-shadow"
                    filter="url(#buildingShadow)"
                  />
                  
                  {/* Main building - clean and modern */}
                  <rect
                    x={buildingX}
                    y={buildingY}
                    width={building.w}
                    height={building.h}
                    rx="6"
                    className={`building ${building.type.toLowerCase()}`}
                    filter={isSelected ? "url(#selectionGlow)" : isHovered ? "url(#hoverGlow)" : "url(#buildingShadow)"}
                    style={{
                      strokeWidth: isSelected ? 2.5 : isHovered ? 2 : 1.5,
                      opacity: isDragging ? 0.8 : 1
                    }}
                  />
                  
                  {/* Subtle depth gradient overlay */}
                  <rect
                    x={buildingX}
                    y={buildingY}
                    width={building.w}
                    height={building.h}
                    rx="6"
                    fill="url(#buildingGradient)"
                    pointerEvents="none"
                  />
                  
                  {/* Building code and name - displayed directly on building */}
                  {building.code && (
                    <>
                      <text
                        x={buildingX + building.w / 2}
                        y={buildingY + 6 + 11}
                        textAnchor="middle"
                        dominantBaseline="middle"
                        fontSize="10"
                        fontWeight="700"
                        fill="white"
                        className="building-code"
                        style={{ 
                          letterSpacing: '0.5px',
                          textShadow: '0 1px 3px rgba(0,0,0,0.8), 0 0 2px rgba(0,0,0,0.5)',
                          pointerEvents: 'none'
                        }}
                      >
                        {building.code}
                      </text>
                      <text
                        x={buildingX + building.w / 2}
                        y={buildingY + 6 + 24}
                        textAnchor="middle"
                        dominantBaseline="middle"
                        className="building-label"
                        fontSize="8"
                        fontWeight="600"
                        fill="white"
                        style={{ 
                          pointerEvents: 'none',
                          textShadow: '0 1px 3px rgba(0,0,0,0.8), 0 0 2px rgba(0,0,0,0.5)'
                        }}
                      >
                        {building.name.length > 18 ? building.name.substring(0, 15) + '...' : building.name}
                      </text>
                    </>
                  )}
                  
                  {/* Clean selection indicator */}
                  {isSelected && (
                    <rect
                      x={buildingX - 5}
                      y={buildingY - 5}
                      width={building.w + 10}
                      height={building.h + 10}
                      rx="8"
                      fill="none"
                      stroke="#c8102e"
                      strokeWidth="3"
                      strokeDasharray="8 4"
                      className="selection-ring"
                      opacity="0.8"
                      style={{ pointerEvents: 'none' }}
                    />
                  )}
                  
                  {/* Edit mode indicator */}
                  {isEditMode && auth?.role === 'admin' && (
                    <rect
                      x={buildingX - 8}
                      y={buildingY - 8}
                      width={building.w + 16}
                      height={building.h + 16}
                      rx="10"
                      fill="none"
                      stroke="#f1be48"
                      strokeWidth="2"
                      strokeDasharray="4 4"
                      opacity="0.6"
                      style={{ pointerEvents: 'none' }}
                    />
                  )}

                  {/* Tour Stop Badge - Visual Tour Routing Feature */}
                  {activeTour && activeTour.buildingIds && activeTour.buildingIds.includes(building.id) && (
                    (() => {
                      const tourIndex = activeTour.buildingIds.indexOf(building.id) + 1
                      const center = getBuildingCenter(building)
                      return (
                        <g>
                          <circle
                            cx={center.x}
                            cy={center.y - 30}
                            r="15"
                            fill="#c8102e"
                            stroke="white"
                            strokeWidth="2"
                            style={{ pointerEvents: 'none' }}
                          />
                          <text
                            x={center.x}
                            y={center.y - 30}
                            textAnchor="middle"
                            dominantBaseline="middle"
                            fill="white"
                            fontSize="12"
                            fontWeight="700"
                            style={{ pointerEvents: 'none' }}
                          >
                            {tourIndex}
                          </text>
                        </g>
                      )
                    })()
                  )}
                </g>
              )
            })}
            </svg>
          </div>

        {/* Enhanced Building Information Panel */}
        <aside className={`building-info-panel-enhanced ${currentBuilding ? 'active' : ''} ${isAnimating ? 'animating' : ''}`}>
          {currentBuilding ? (
            <div className="building-details-enhanced">
              <div className="building-header-enhanced">
                <div>
                  <h2 className="building-name-enhanced">{currentBuilding.name}</h2>
                  {currentBuilding.code && (
                    <p style={{ 
                      margin: '0.25rem 0 0 0', 
                      fontSize: '0.875rem', 
                      color: 'var(--text-muted)',
                      fontFamily: 'monospace',
                      fontWeight: '600'
                    }}>
                      {currentBuilding.code}
                    </p>
                  )}
                </div>
                <span className={`building-type-badge-enhanced ${currentBuilding.type.toLowerCase().replace(' ', '-')}`}>
                  {currentBuilding.type}
                </span>
              </div>
              
              <p className="building-description-enhanced">{currentBuilding.desc}</p>
              
              <div className="building-info-grid-enhanced">
                <div className="info-item-enhanced">
                  <div className="info-icon">📅</div>
                  <div>
                    <div className="info-label">Year Built</div>
                    <div className="info-value">{currentBuilding.yearBuilt}</div>
                  </div>
                </div>
                <div className="info-item-enhanced">
                  <div className="info-icon">🏢</div>
                  <div>
                    <div className="info-label">Floors</div>
                    <div className="info-value">{currentBuilding.floors}</div>
                  </div>
                </div>
                <div className="info-item-enhanced">
                  <div className="info-icon">👥</div>
                  <div>
                    <div className="info-label">Capacity</div>
                    <div className="info-value">{currentBuilding.capacity}</div>
                  </div>
                </div>
                <div className="info-item-enhanced" style={{ gridColumn: '1 / -1' }}>
                  <div className="info-icon">🕒</div>
                  <div>
                    <div className="info-label">Hours</div>
                    <div className="info-value">{currentBuilding.hours}</div>
                  </div>
                </div>
              </div>
              
              <div className="departments-section-enhanced">
                <h3 style={{ 
                  fontSize: '0.875rem', 
                  fontWeight: '600', 
                  color: 'var(--text)',
                  marginBottom: '0.75rem',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }}>
                  Departments & Services
                </h3>
                <div className="departments-list-enhanced">
                  {currentBuilding.departments.map((dept, index) => (
                    <span key={index} className="department-tag-enhanced">{dept}</span>
                  ))}
                </div>
              </div>
              
              {selectedBuilding && (
                <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.5rem', flexWrap: 'wrap' }}>
                  <button 
                    className="btn primary"
                    onClick={() => navigate(`/building/${selectedBuilding.id}`)}
                    style={{ flex: 1, minWidth: '150px' }}
                  >
                    View Full Details →
                  </button>
                  <button 
                    className="btn secondary close-btn-enhanced"
                    onClick={() => setSelectedBuilding(null)}
                    aria-label="Close building details"
                    style={{ flex: 1, minWidth: '150px' }}
                  >
                    Close
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="default-info-enhanced">
              <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
                <div style={{
                  width: '64px',
                  height: '64px',
                  margin: '0 auto 1rem',
                  background: 'linear-gradient(135deg, rgba(200,16,46,0.1) 0%, rgba(241,190,72,0.1) 100%)',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '2rem'
                }}>
                  🗺️
                </div>
                <h2 style={{ 
                  fontSize: '1.5rem', 
                  fontWeight: '600', 
                  color: 'var(--cardinal)',
                  marginBottom: '0.5rem'
                }}>
                  Explore Campus Buildings
                </h2>
                <p style={{ 
                  color: 'var(--text-muted)', 
                  fontSize: '0.95rem',
                  lineHeight: '1.6'
                }}>
                  Click or hover over any building on the map to discover detailed information about facilities, departments, and services.
                </p>
              </div>
              <div className="map-legend-enhanced">
                <h3 style={{ 
                  fontSize: '0.875rem', 
                  fontWeight: '600', 
                  color: 'var(--text)',
                  marginBottom: '1rem',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }}>
                  Building Types
                </h3>
                <div className="legend-grid">
                  <div className="legend-item-enhanced">
                    <span className="legend-dot-enhanced academic"></span>
                    <span>Academic</span>
                  </div>
                  <div className="legend-item-enhanced">
                    <span className="legend-dot-enhanced administration"></span>
                    <span>Administration</span>
                  </div>
                  <div className="legend-item-enhanced">
                    <span className="legend-dot-enhanced student-life"></span>
                    <span>Student Life</span>
                  </div>
                  <div className="legend-item-enhanced">
                    <span className="legend-dot-enhanced athletics"></span>
                    <span>Athletics</span>
                  </div>
                  <div className="legend-item-enhanced">
                    <span className="legend-dot-enhanced residence"></span>
                    <span>Residence</span>
                  </div>
                  <div className="legend-item-enhanced">
                    <span className="legend-dot-enhanced landmark"></span>
                    <span>Landmark</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </aside>
        </div>
      )}

      {/* Notification Toast */}
      {notification && (
        <div
          style={{
            position: 'fixed',
            bottom: '2rem',
            right: '2rem',
            background: notification.type === 'success' ? '#4caf50' : '#f44336',
            color: 'white',
            padding: '1rem 1.5rem',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            zIndex: 10000,
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            maxWidth: '400px',
            animation: 'slideInUp 0.3s ease-out'
          }}
        >
          <span style={{ fontSize: '1.25rem' }}>
            {notification.type === 'success' ? '✓' : '⚠'}
          </span>
          <span style={{ fontSize: '0.95rem', fontWeight: '500' }}>
            {notification.message}
          </span>
        </div>
      )}
    </section>
  )
}
