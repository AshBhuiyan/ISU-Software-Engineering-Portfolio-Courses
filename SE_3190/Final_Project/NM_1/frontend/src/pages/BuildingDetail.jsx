import React, { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'

export default function BuildingDetail({ auth }) {
  const { id } = useParams()
  const navigate = useNavigate()
  const [building, setBuilding] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedFloor, setSelectedFloor] = useState(null)
  const [roomInput, setRoomInput] = useState('')
  const [showEditMedia, setShowEditMedia] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)
  const [newMediaFile, setNewMediaFile] = useState(null)
  const [newMediaType, setNewMediaType] = useState(null) // 'image' or 'video'
  const [newVideoUrl, setNewVideoUrl] = useState('')
  const [floorPlanFiles, setFloorPlanFiles] = useState({}) // Object mapping floor index to File
  const [showFloorPlanSection, setShowFloorPlanSection] = useState(false)
  const [numFloorsToAdd, setNumFloorsToAdd] = useState(1)
  // Floor plan data for Parks Library
  const parksLibraryFloors = {
    lower: {
      name: 'Lower Level',
      image: '/assets/images/parkslower.jpg',
      description: 'Maps, media, and microforms; Studio 2B; IT Solution Center (Room 31); Library IT Help Desk; International Nest; and Group Study Room 03.',
      roomRange: '000-099'
    },
    level1: {
      name: 'Level 1',
      image: '/assets/images/parksfloor.jpeg',
      description: 'Main desk, Fireplace Reading Room, Bookends Café, classrooms (134–198), Central Parks, Tech Lending, study rooms, and restrooms.',
      roomRange: '100-199'
    },
    level2: {
      name: 'Level 2',
      image: '/assets/images/parksfloor2.png',
      description: 'Study Abroad Center 281, Reading Room 298, individual study rooms, printers/computers, and restrooms.',
      roomRange: '200-299'
    },
    level3: {
      name: 'Level 3',
      image: '/assets/images/parksfloor3.png',
      description: "Dean's Office 302, group study rooms, study carrels, call numbers A–DR, and restrooms.",
      roomRange: '300-399'
    },
    level4: {
      name: 'Level 4',
      image: '/assets/images/parksfloor4.png',
      description: 'Special Collections & University Archives 403, classrooms 405–450, preservation department, and restrooms.',
      roomRange: '400-499'
    }
  }

  // Floor plan data for Carver Hall
  const carverHallFloors = {
    ground: {
      name: 'Lower Level',
      image: '/assets/images/Carver0F.png',
      description: 'Ground Floor (0001–0099): Main lecture hall 0001 and surrounding corridors; emergency exits as posted.',
      roomRange: '0001-0099'
    },
    level1: {
      name: 'Level 1',
      image: '/assets/images/Carver1F.png',
      description: 'Level 1 (0100–0199): First-floor classrooms and central atrium.',
      roomRange: '0100-0199'
    },
    level2: {
      name: 'Level 2',
      image: '/assets/images/Carver2F.png',
      description: 'Level 2 (0200–0299): Second-floor classrooms around the balcony ring.',
      roomRange: '0200-0299'
    },
    level3: {
      name: 'Level 3',
      image: '/assets/images/Carver3F.png',
      description: 'Level 3 (0300–0399): Third-floor classrooms and offices along the perimeter.',
      roomRange: '0300-0399'
    },
    level4: {
      name: 'Level 4',
      image: '/assets/images/Carver4F.png',
      description: 'Level 4 (0400–0499): Fourth-floor classrooms, labs, and offices.',
      roomRange: '0400-0499'
    }
  }

  // Get floor plans based on building
  const getFloorPlans = () => {
    if (!building) return null
    
    // If building has floorPlans from database, use those
    if (building.floorPlans && building.floorPlans.length > 0) {
      // Convert array to object format for compatibility
      const floorPlansObj = {}
      building.floorPlans.forEach((imageUrl, index) => {
        const floorKey = index === 0 ? 'lower' : `level${index}`
        floorPlansObj[floorKey] = {
          name: index === 0 ? 'Lower Level' : `Level ${index}`,
          image: imageUrl,
          description: `Floor ${index + 1} of ${building.name}`
        }
      })
      return floorPlansObj
    }
    
    // Otherwise, use hardcoded floor plans for Parks Library and Carver Hall
    if (building.id === 'library' || building.name.toLowerCase().includes('parks')) {
      return parksLibraryFloors
    }
    if (building.id === 'carver' || building.name.toLowerCase().includes('carver')) {
      return carverHallFloors
    }
    return null
  }

  useEffect(() => {
    const fetchBuilding = async () => {
      try {
        setLoading(true)
        setError(null)
        const response = await fetch(`/api/buildings/${id}`)
        if (!response.ok) {
          throw new Error('Building not found')
        }
        const data = await response.json()
        if (data.success && data.building) {
          setBuilding(data.building)
        } else {
          throw new Error('Invalid response format')
        }
      } catch (err) {
        setError(err.message)
        console.error('Error fetching building:', err)
      } finally {
        setLoading(false)
      }
    }

    if (id) {
      fetchBuilding()
    }
  }, [id])

  const handleRoomInput = () => {
    const roomStr = roomInput.trim()
    const room = parseInt(roomStr)
    if (isNaN(room)) {
      alert('Please enter a valid room number')
      return
    }

    const floorPlans = getFloorPlans()
    if (!floorPlans) {
      alert('Floor plans not available for this building')
      return
    }

    // Determine floor based on room number
    let floorKey = null
    let floorIndex = -1
    
    // Special handling for Parks Library and Carver Hall (hardcoded)
    if (building.id === 'library' || building.name.toLowerCase().includes('parks')) {
      if (room < 100) floorKey = 'lower'
      else if (room < 200) floorKey = 'level1'
      else if (room < 300) floorKey = 'level2'
      else if (room < 400) floorKey = 'level3'
      else if (room < 500) floorKey = 'level4'
    } else if (building.id === 'carver' || building.name.toLowerCase().includes('carver')) {
      if (room < 100) floorKey = 'ground'
      else if (room < 200) floorKey = 'level1'
      else if (room < 300) floorKey = 'level2'
      else if (room < 400) floorKey = 'level3'
      else if (room < 500) floorKey = 'level4'
    } else {
      // Generic algorithm for buildings with database floorPlans
      // Rooms starting with 0 (000-099) = floor 0 (ground/lower)
      // Rooms starting with 1 (100-199) = floor 1
      // Rooms starting with 2 (200-299) = floor 2, etc.
      
      // Get the first digit(s) of the room number
      // Handle both 3-digit (050) and 4-digit (1050) room numbers
      let firstDigit = 0
      if (roomStr.length >= 3) {
        // For 3-digit: "050" -> first digit is 0, "150" -> first digit is 1
        // For 4-digit: "1050" -> first two digits are 10, but we want floor 1
        // So we use Math.floor(room / 100) to get the floor number
        firstDigit = Math.floor(room / 100)
      } else {
        // For 1-2 digit numbers, treat as ground floor
        firstDigit = 0
      }
      
      // Map to floor index (0 = ground/lower, 1 = level 1, etc.)
      floorIndex = firstDigit
      
      // Convert to floorKey format
      if (floorIndex === 0) {
        floorKey = 'lower'
      } else {
        floorKey = `level${floorIndex}`
      }
    }

    // Check if the floor exists in floorPlans
    if (floorKey && floorPlans[floorKey]) {
      setSelectedFloor(floorKey)
      // Scroll to the floor display if it exists
      setTimeout(() => {
        const floorDisplay = document.querySelector('[data-floor-display]')
        if (floorDisplay) {
          floorDisplay.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }, 100)
    } else {
      // If using database floorPlans, check if floorIndex is valid
      if (building.floorPlans && building.floorPlans.length > 0) {
        if (floorIndex >= 0 && floorIndex < building.floorPlans.length) {
          // Floor exists, set it
          if (floorIndex === 0) {
            setSelectedFloor('lower')
          } else {
            setSelectedFloor(`level${floorIndex}`)
          }
          setTimeout(() => {
            const floorDisplay = document.querySelector('[data-floor-display]')
            if (floorDisplay) {
              floorDisplay.scrollIntoView({ behavior: 'smooth', block: 'center' })
            }
          }, 100)
        } else {
          alert(`Room ${room} not found. This building has ${building.floorPlans.length} floor${building.floorPlans.length !== 1 ? 's' : ''} (0-${building.floorPlans.length - 1}).`)
        }
      } else {
        alert('Room not found in this building')
      }
    }
  }

  const handleFloorSelect = (floorKey) => {
    setSelectedFloor(floorKey)
  }

  if (loading) {
    return (
      <section className="centered">
        <div className="card">
          <p className="muted">Loading building details...</p>
        </div>
      </section>
    )
  }

  if (error || !building) {
    return (
      <section className="centered">
        <div className="card">
          <h1 className="h1" style={{ color: 'var(--cardinal)' }}>Building Not Found</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>{error || 'The building you are looking for does not exist.'}</p>
          <Link to="/campus" className="btn primary">Back to Campus Map</Link>
        </div>
      </section>
    )
  }

  const floorPlans = getFloorPlans()
  const isParksLibrary = building.id === 'library' || building.name.toLowerCase().includes('parks')
  const hasVideo = building.video && building.video.trim() !== ''
  const heroImage = building.image || (building.id === 'carver' ? '/assets/images/Carverhall.jpg' : '/assets/images/parks1.jpeg')

  // Helper function to check if URL is YouTube and convert to embed format
  const getVideoSource = (url) => {
    if (!url) return null
    
    // Check if it's a YouTube URL
    const youtubeRegex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/
    const match = url.match(youtubeRegex)
    
    if (match) {
      const videoId = match[1]
      return {
        type: 'youtube',
        embedUrl: `https://www.youtube.com/embed/${videoId}`
      }
    }
    
    // Check if it's already an embed URL
    if (url.includes('youtube.com/embed/')) {
      return {
        type: 'youtube',
        embedUrl: url
      }
    }
    
    // Regular video file URL
    return {
      type: 'video',
      url: url
    }
  }

  const videoSource = hasVideo ? getVideoSource(building.video) : null

  // Handle media file upload (image or video)
  const handleMediaUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) {
      setNewMediaFile(null)
      setNewMediaType(null)
      return
    }
    
    // Determine if it's an image or video
    const isImage = file.type.startsWith('image/')
    const isVideo = file.type.startsWith('video/')
    
    if (isImage) {
      setNewMediaFile(file)
      setNewMediaType('image')
    } else if (isVideo) {
      setNewMediaFile(file)
      setNewMediaType('video')
      // Clear video URL input when video file is selected
      setNewVideoUrl('')
    } else {
      alert('Please select an image or video file')
      e.target.value = '' // Clear the input
      setNewMediaFile(null)
      setNewMediaType(null)
    }
  }

  // Handle save media
  const handleSaveMedia = async () => {
    try {
      console.log('Save media clicked', { 
        newMediaFile: !!newMediaFile, 
        newVideoUrl, 
        floorPlanFiles: Object.keys(floorPlanFiles).length 
      })
      setUploading(true)
      setUploadError(null)

      let imageUrl = building.image
      let videoUrl = building.video

      // Upload new media file if selected (image or video)
      if (newMediaFile) {
        const formData = new FormData()
        // Use 'file' as the field name to match backend expectation
        formData.append('file', newMediaFile)

        console.log('Uploading file:', {
          name: newMediaFile.name,
          type: newMediaFile.type,
          size: newMediaFile.size,
          fieldName: 'file'
        })

        const uploadResponse = await fetch('/api/upload', {
          method: 'POST',
          body: formData
          // Note: Don't set Content-Type header - browser will set it with boundary for FormData
        })

        // Check if response is JSON before parsing
        const contentType = uploadResponse.headers.get('content-type')
        if (!contentType || !contentType.includes('application/json')) {
          const text = await uploadResponse.text()
          throw new Error(`Server error: ${text.substring(0, 200)}`)
        }

        const uploadData = await uploadResponse.json()

        if (!uploadResponse.ok || !uploadData.success) {
          throw new Error(uploadData.message || `Failed to upload ${newMediaType}`)
        }

        // Update the appropriate URL based on file type
        if (newMediaType === 'image') {
          imageUrl = uploadData.url
        } else if (newMediaType === 'video') {
          videoUrl = uploadData.url
        }
      }

      // Handle video URL input (only if no video file was uploaded)
      if (!newMediaFile || newMediaType !== 'video') {
        if (newVideoUrl !== undefined && newVideoUrl.trim() !== '') {
          // Use video URL if provided
          videoUrl = newVideoUrl.trim()
        } else if (newVideoUrl === '') {
          // Explicitly clear video if URL is empty (and no file was uploaded)
          if (!newMediaFile || newMediaType !== 'video') {
            videoUrl = null
          }
        }
      }

      // Upload floor plan images
      let updatedFloorPlans = building.floorPlans ? [...building.floorPlans] : []
      const floorPlanIndices = Object.keys(floorPlanFiles).map(Number).sort((a, b) => a - b)
      
      for (const floorIndex of floorPlanIndices) {
        const file = floorPlanFiles[floorIndex]
        if (file) {
          const formData = new FormData()
          formData.append('file', file)

          const uploadResponse = await fetch('/api/upload', {
            method: 'POST',
            body: formData
          })

          const contentType = uploadResponse.headers.get('content-type')
          if (!contentType || !contentType.includes('application/json')) {
            const text = await uploadResponse.text()
            throw new Error(`Server error uploading floor plan ${floorIndex + 1}: ${text.substring(0, 200)}`)
          }

          const uploadData = await uploadResponse.json()

          if (!uploadResponse.ok || !uploadData.success) {
            throw new Error(uploadData.message || `Failed to upload floor plan ${floorIndex + 1}`)
          }

          // Update the floor plan at this index
          if (floorIndex < updatedFloorPlans.length) {
            updatedFloorPlans[floorIndex] = uploadData.url
          } else {
            // If index is beyond current array, extend it
            while (updatedFloorPlans.length <= floorIndex) {
              updatedFloorPlans.push('/assets/images/Iowa_State_Cyclones_logo.svg.png')
            }
            updatedFloorPlans[floorIndex] = uploadData.url
          }
        }
      }

      // Update building with new media
      const updateData = {
        image: imageUrl || building.image,
        video: videoUrl !== undefined ? videoUrl : building.video
      }

      // Always update floorPlans if building has floorPlans or we're uploading new ones
      // This ensures existing floor plans are preserved even if we're only updating other media
      if (floorPlanIndices.length > 0 || building.floorPlans) {
        // If we have existing floor plans but no new uploads, keep the existing ones
        if (floorPlanIndices.length === 0 && building.floorPlans) {
          updateData.floorPlans = building.floorPlans
        } else {
          updateData.floorPlans = updatedFloorPlans
        }
      }

      console.log('Updating building with:', updateData)

      const response = await fetch(`/api/buildings/${building.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updateData)
      })

      const data = await response.json()

      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Failed to update building media')
      }

      // Refresh building data
      const fetchResponse = await fetch(`/api/buildings/${id}`)
      const fetchData = await fetchResponse.json()
      if (fetchData.success) {
        setBuilding(fetchData.building)
      }

      setShowEditMedia(false)
      setNewMediaFile(null)
      setNewMediaType(null)
      setNewVideoUrl('')
      setFloorPlanFiles({})
      setShowFloorPlanSection(false)
      setNumFloorsToAdd(1)
    } catch (err) {
      setUploadError(err.message)
      console.error('Error updating media:', err)
    } finally {
      setUploading(false)
    }
  }

  return (
    <section className="campus">
      {/* Hero Section */}
      <div style={{ position: 'relative', width: '100%', marginBottom: '2rem' }}>
        {hasVideo && videoSource ? (
          <div style={{ textAlign: 'center', position: 'relative', display: 'inline-block', width: '100%' }}>
            {auth?.role === 'admin' && (
              <button
                onClick={() => {
                  setShowEditMedia(true)
                  setNewVideoUrl(building.video || '')
                  setNewMediaFile(null)
                  setNewMediaType(null)
                  setFloorPlanFiles({})
                  setShowFloorPlanSection(false)
                  setNumFloorsToAdd(1)
                }}
                className="btn primary"
                style={{
                  position: 'absolute',
                  top: '1rem',
                  right: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  zIndex: 10
                }}
              >
                ✏️ Edit Media
              </button>
            )}
            {videoSource.type === 'youtube' ? (
              <iframe
                src={videoSource.embedUrl}
                title={building.name}
                style={{
                  width: '85%',
                  maxHeight: '550px',
                  aspectRatio: '16/9',
                  borderRadius: '14px',
                  boxShadow: '0 6px 18px rgba(0,0,0,0.25)',
                  border: 'none'
                }}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
              />
            ) : (
              <video
                src={videoSource.url}
                autoPlay
                loop
                muted
                playsInline
                controls
                style={{
                  width: '85%',
                  maxHeight: '550px',
                  borderRadius: '14px',
                  boxShadow: '0 6px 18px rgba(0,0,0,0.25)',
                  objectFit: 'cover'
                }}
              />
            )}
            {building.id === 'library' && videoSource.type === 'video' && (
              <div style={{
                position: 'absolute',
                bottom: '10px',
                right: '20px',
                background: 'rgba(0,0,0,0.6)',
                color: '#fff',
                padding: '6px 10px',
                borderRadius: '6px',
                fontSize: '0.8rem',
                fontStyle: 'italic'
              }}>
                Video credit: <a href="https://www.facebook.com/iowastateu.library/videos/1990034604835051/" target="_blank" rel="noopener noreferrer" style={{ color: '#aad4ff', textDecoration: 'none' }}>Iowa State University Library</a>
              </div>
            )}
          </div>
        ) : (
          <div style={{ textAlign: 'center', position: 'relative' }}>
            <img
              src={heroImage}
              alt={building.name}
              style={{
                width: '85%',
                maxHeight: '700px',
                borderRadius: '14px',
                boxShadow: '0 6px 18px rgba(0,0,0,0.25)',
                objectFit: 'cover'
              }}
            />
            {auth?.role === 'admin' && (
              <button
                onClick={() => {
                  setShowEditMedia(true)
                  setNewVideoUrl(building.video || '')
                  setNewMediaFile(null)
                  setNewMediaType(null)
                  setFloorPlanFiles({})
                  setShowFloorPlanSection(false)
                  setNumFloorsToAdd(1)
                }}
                className="btn primary"
                style={{
                  position: 'absolute',
                  top: '1rem',
                  right: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem'
                }}
              >
                ✏️ Edit Media
              </button>
            )}
          </div>
        )}
      </div>

      {/* Image Gallery */}
      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">{building.name}</h2>
        <p className="muted" style={{ marginTop: '0.5rem' }}>{building.description}</p>
        
        {building.gallery && building.gallery.length > 0 && (
          <div style={{ 
            display: 'flex', 
            overflowX: 'auto', 
            gap: '15px', 
            padding: '1rem',
            marginTop: '1.5rem',
            scrollSnapType: 'x mandatory'
          }}>
            {building.gallery.map((imageUrl, index) => (
              <img 
                key={index}
                src={imageUrl} 
                alt={`${building.name} ${index + 1}`} 
                style={{ 
                  height: '230px', 
                  borderRadius: '12px', 
                  boxShadow: '0 3px 8px rgba(0,0,0,0.2)',
                  objectFit: 'cover'
                }} 
              />
            ))}
          </div>
        )}
      </div>

      {/* Info Strip */}
      <div className="card" style={{ marginTop: '2rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
          <div>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>🕒 Hours</h3>
            <p className="muted">{building.hours}</p>
          </div>
          <div>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>👥 Capacity</h3>
            <p className="muted">{building.capacity}</p>
          </div>
          <div>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>📅 Year Built</h3>
            <p className="muted">{building.yearBuilt}</p>
          </div>
          <div>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>🏢 Floors</h3>
            <p className="muted">{building.floors}</p>
          </div>
        </div>

        {building.departments && building.departments.length > 0 && (
          <div style={{ marginTop: '1.5rem' }}>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.75rem' }}>🏛️ Departments & Services</h3>
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
              {building.departments.map((dept, index) => (
                <span
                  key={index}
                  style={{
                    padding: '0.5rem 1rem',
                    background: 'rgba(200,16,46,0.1)',
                    borderRadius: '0.5rem',
                    fontSize: '0.9rem',
                    color: 'var(--text)'
                  }}
                >
                  {dept}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Room Input & Floor Plan Viewer */}
      {floorPlans && (
        <div className="card" style={{ marginTop: '2rem' }}>
          <h2 className="h2">Floor Plans</h2>
          
          {/* Room Input */}
          <div style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem', justifyContent: 'center', alignItems: 'center', flexWrap: 'wrap' }}>
            <input
              type="text"
              placeholder="Enter Room #"
              value={roomInput}
              onChange={(e) => setRoomInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleRoomInput()}
              style={{
                padding: '0.75rem 1rem',
                borderRadius: '0.5rem',
                border: '1px solid var(--border)',
                fontSize: '1rem',
                minWidth: '200px'
              }}
            />
            <button onClick={handleRoomInput} className="btn primary">
              Go
            </button>
          </div>

          {/* Floor Selection Buttons */}
          <div style={{ marginTop: '2rem', textAlign: 'center' }}>
            <h3 className="h3" style={{ marginBottom: '1rem' }}>Select a Level</h3>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
              {Object.keys(floorPlans).map((floorKey) => (
                <button
                  key={floorKey}
                  onClick={() => handleFloorSelect(floorKey)}
                  className={selectedFloor === floorKey ? 'btn primary' : 'btn secondary'}
                  style={{ padding: '0.75rem 1.5rem' }}
                >
                  {floorPlans[floorKey].name}
                </button>
              ))}
            </div>
          </div>

          {/* Floor Display */}
          {selectedFloor && floorPlans[selectedFloor] && (
            <div style={{ marginTop: '2rem', textAlign: 'center' }} data-floor-display>
              <h3 className="h3" style={{ color: 'var(--cardinal)', marginBottom: '0.5rem' }}>
                {floorPlans[selectedFloor].name}
              </h3>
              <p className="muted" style={{ marginBottom: '1rem' }}>
                {floorPlans[selectedFloor].description}
              </p>
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <img
                  src={floorPlans[selectedFloor].image}
                  alt={`${floorPlans[selectedFloor].name} Floor Plan`}
                  style={{
                    maxWidth: '80%',
                    borderRadius: '0.5rem',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    background: 'white',
                    padding: '1rem'
                  }}
                />
              </div>
            </div>
          )}
        </div>
      )}

      {/* Back Button */}
      <div style={{ marginTop: '2rem', textAlign: 'center' }}>
        <Link to="/campus" className="btn secondary">
          ← Back to Campus Map
        </Link>
      </div>
      {/* Edit Media Modal */}
      {showEditMedia && (
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
          <div className="card" style={{ maxWidth: '500px', width: '100%', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1.5rem' }}>
              <h2 className="h2" style={{ margin: 0 }}>Edit Media</h2>
              <button
                onClick={() => {
                  setShowEditMedia(false)
                  setNewMediaFile(null)
                  setNewMediaType(null)
                  setNewVideoUrl('')
                  setFloorPlanFiles({})
                  setShowFloorPlanSection(false)
                  setNumFloorsToAdd(1)
                  setUploadError(null)
                }}
                className="btn ghost"
                style={{ padding: '0.5rem 1rem' }}
              >
                ✕
              </button>
            </div>

            {uploadError && (
              <div style={{
                padding: '1rem',
                background: '#fee',
                border: '1px solid #fcc',
                borderRadius: '8px',
                marginBottom: '1rem',
                color: '#c33'
              }}>
                {uploadError}
              </div>
            )}

            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                Upload Image or Video File
              </label>
              <input
                type="file"
                accept="image/*,video/mp4,video/webm,video/ogg"
                onChange={handleMediaUpload}
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  fontSize: '0.9rem'
                }}
              />
              {newMediaFile && (
                <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                  Selected: {newMediaFile.name} ({newMediaType === 'image' ? 'Image' : 'Video'})
                </p>
              )}
              {building.image && (!newMediaFile || newMediaType !== 'image') && (
                <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                  Current Image: {building.image}
                </p>
              )}
              {building.video && (!newMediaFile || newMediaType !== 'video') && (
                <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                  Current Video: {building.video}
                </p>
              )}
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                Or Enter Video URL
              </label>
              <input
                type="text"
                value={newVideoUrl}
                onChange={(e) => {
                  setNewVideoUrl(e.target.value)
                  // Clear media file when URL is entered
                  if (e.target.value && newMediaType === 'video') {
                    setNewMediaFile(null)
                    setNewMediaType(null)
                  }
                }}
                placeholder="Enter video URL (e.g., /assets/videos/video.mp4 or YouTube URL)"
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  fontSize: '0.9rem'
                }}
              />
              <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                Supports direct video files (MP4) or YouTube URLs. Leave empty to remove video. Use either file upload or URL (not both).
              </p>
            </div>

            {/* Floor Plan Upload Section */}
            <div style={{ marginBottom: '1.5rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border)' }}>
              <div 
                style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center',
                  cursor: 'pointer',
                  marginBottom: showFloorPlanSection ? '1rem' : '0'
                }}
                onClick={() => setShowFloorPlanSection(!showFloorPlanSection)}
              >
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: '600' }}>Floor Plan Upload</h3>
                <span style={{ fontSize: '1.2rem', color: 'var(--text-muted)' }}>
                  {showFloorPlanSection ? '▼' : '▶'}
                </span>
              </div>
              
              {showFloorPlanSection && (
                <div>
                  <p style={{ marginBottom: '1rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                    Upload floor plan images for each floor. This building has {building.floors} floor{building.floors !== 1 ? 's' : ''}.
                  </p>
                  
                  <div style={{ marginBottom: '1.5rem' }}>
                    <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500', fontSize: '0.9rem' }}>
                      How many floors do you wish to add? (Max: 10)
                    </label>
                    <input
                      type="number"
                      min="1"
                      max="10"
                      value={numFloorsToAdd}
                      onChange={(e) => {
                        const value = parseInt(e.target.value) || 1
                        const clampedValue = Math.min(Math.max(value, 1), 10)
                        setNumFloorsToAdd(clampedValue)
                        // Clear floor plan files that are beyond the new number
                        setFloorPlanFiles(prev => {
                          const newFiles = {}
                          Object.keys(prev).forEach(key => {
                            if (parseInt(key) < clampedValue) {
                              newFiles[key] = prev[key]
                            }
                          })
                          return newFiles
                        })
                      }}
                      style={{
                        width: '100%',
                        padding: '0.75rem',
                        border: '1px solid var(--border)',
                        borderRadius: '8px',
                        fontSize: '0.9rem'
                      }}
                    />
                  </div>
                  
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    {Array.from({ length: numFloorsToAdd }, (_, index) => (
                      <div key={index} style={{ marginBottom: '0.5rem' }}>
                        <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500', fontSize: '0.9rem' }}>
                          Floor {index + 1} {index === 0 && numFloorsToAdd > 1 ? '(Ground/Lower Level)' : ''}
                        </label>
                        <input
                          type="file"
                          accept="image/*"
                          onChange={(e) => {
                            const file = e.target.files[0]
                            if (file) {
                              setFloorPlanFiles(prev => ({
                                ...prev,
                                [index]: file
                              }))
                            } else {
                              setFloorPlanFiles(prev => {
                                const newFiles = { ...prev }
                                delete newFiles[index]
                                return newFiles
                              })
                            }
                          }}
                          style={{
                            width: '100%',
                            padding: '0.75rem',
                            border: '1px solid var(--border)',
                            borderRadius: '8px',
                            fontSize: '0.9rem'
                          }}
                        />
                        {floorPlanFiles[index] && (
                          <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                            Selected: {floorPlanFiles[index].name}
                          </p>
                        )}
                        {building.floorPlans && building.floorPlans[index] && !floorPlanFiles[index] && (
                          <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                            Current: {building.floorPlans[index]}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
              <button
                onClick={() => {
                  setShowEditMedia(false)
                  setNewMediaFile(null)
                  setNewMediaType(null)
                  setNewVideoUrl('')
                  setFloorPlanFiles({})
                  setShowFloorPlanSection(false)
                  setNumFloorsToAdd(1)
                  setUploadError(null)
                }}
                className="btn secondary"
                disabled={uploading}
              >
                Cancel
              </button>
              <button
                onClick={(e) => {
                  e.preventDefault()
                  console.log('Save button clicked', { 
                    uploading, 
                    hasNewMedia: !!newMediaFile, 
                    videoChanged: newVideoUrl !== (building.video || ''),
                    hasFloorPlans: Object.keys(floorPlanFiles).length > 0
                  })
                  if (!uploading) {
                    handleSaveMedia()
                  }
                }}
                className="btn primary"
                disabled={uploading || (!newMediaFile && newVideoUrl === (building.video || '') && Object.keys(floorPlanFiles).length === 0)}
              >
                {uploading ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
