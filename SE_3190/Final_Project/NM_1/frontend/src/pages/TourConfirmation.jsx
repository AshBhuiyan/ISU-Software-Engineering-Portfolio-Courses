import React from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

export default function TourConfirmation({ auth }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { tour, mode } = location.state || {}

  // If no tour data, redirect to tours page
  if (!tour) {
    return (
      <section className="centered">
        <div className="card form-card">
          <h1 className="h1" style={{ color: 'var(--cardinal)' }}>No Tour Data</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            No tour information found. Please create a tour first.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/tours/create" className="btn primary">Create Tour</Link>
            <Link to="/tours" className="btn secondary">My Tours</Link>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section className="centered">
      <div className="card form-card" style={{ maxWidth: '700px' }}>
        {/* Success Icon */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            width: '80px',
            height: '80px',
            margin: '0 auto 1.5rem',
            background: 'linear-gradient(135deg, rgba(200,16,46,0.1) 0%, rgba(241,190,72,0.1) 100%)',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '3rem'
          }}>
            ✅
          </div>
          <h1 className="h1" style={{ color: 'var(--cardinal)', marginBottom: '0.5rem' }}>
            Tour {mode === 'created' ? 'Created' : 'Saved'} Successfully!
          </h1>
          <p className="muted" style={{ fontSize: '1.1rem' }}>
            Your campus tour has been saved and is ready to use.
          </p>
        </div>

        {/* Tour Summary */}
        <div style={{
          background: 'linear-gradient(135deg, rgba(200,16,46,0.05) 0%, rgba(241,190,72,0.05) 100%)',
          borderRadius: '0.5rem',
          padding: '1.5rem',
          marginBottom: '2rem',
          border: '1px solid rgba(200,16,46,0.2)'
        }}>
          <h2 className="h2" style={{ color: 'var(--cardinal)', marginBottom: '1rem' }}>
            {tour.name}
          </h2>
          
          {tour.description && (
            <p className="muted" style={{ marginBottom: '1rem' }}>
              {tour.description}
            </p>
          )}

          <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginTop: '1rem' }}>
            <div>
              <strong style={{ color: 'var(--text)' }}>Tour Stops:</strong>
              <p className="muted" style={{ margin: '0.25rem 0 0 0' }}>
                {tour.buildingIds?.length || 0} building{tour.buildingIds?.length !== 1 ? 's' : ''}
              </p>
            </div>
            <div>
              <strong style={{ color: 'var(--text)' }}>Created:</strong>
              <p className="muted" style={{ margin: '0.25rem 0 0 0' }}>
                {new Date(tour.createdAt).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric'
                })}
              </p>
            </div>
          </div>
        </div>

        {/* Next Steps */}
        <div style={{ marginBottom: '2rem' }}>
          <h3 className="h3" style={{ marginBottom: '1rem' }}>What's Next?</h3>
          <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem' }}>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>View your tour details in <Link to="/tours" style={{ color: 'var(--cardinal)', textDecoration: 'none', fontWeight: '600' }}>My Tours</Link></span>
            </li>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>Explore the buildings on the <Link to="/campus" style={{ color: 'var(--cardinal)', textDecoration: 'none', fontWeight: '600' }}>Campus Map</Link></span>
            </li>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>Create another tour to plan more campus exploration</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/tours" className="btn primary">
            View My Tours
          </Link>
          <Link to="/tours/create" className="btn secondary">
            Create Another Tour
          </Link>
          <Link to="/campus" className="btn ghost">
            View Campus Map
          </Link>
        </div>

        {/* Back to Home */}
        <div style={{ textAlign: 'center', marginTop: '2rem', paddingTop: '2rem', borderTop: '1px solid var(--border)' }}>
          <Link to="/" style={{ color: 'var(--text-muted)', textDecoration: 'none', fontSize: '0.9rem' }}>
            ← Return to Home
          </Link>
        </div>
      </div>
    </section>
  )
}
