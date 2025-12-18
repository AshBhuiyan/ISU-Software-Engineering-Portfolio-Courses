import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AdminDashboard({ auth }) {
  const navigate = useNavigate()
  const [stats, setStats] = useState({
    totalUsers: 1250,
    totalBuildings: 10,
    activeTours: 8,
  })

  // Redirect if not authenticated or not admin
  useEffect(() => {
    if (!auth.isAuthenticated || auth.role !== 'admin') {
      // Allow the component to render the "Not Authorized" message
      return
    }
  }, [auth])

  // Show "Not Authorized" if not admin
  if (!auth.isAuthenticated || auth.role !== 'admin') {
    return (
      <section className="centered">
        <div className="card form-card" role="region" aria-labelledby="unauthorized-title">
          <h1 id="unauthorized-title" className="h1" style={{ color: 'var(--cardinal)' }}>Not Authorized</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            This page is restricted to administrators only. You must be logged in with an admin account to access the Admin Dashboard.
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

  return (
    <section className="campus">
      <header className="campus-header">
        <h1 className="display">Admin Dashboard</h1>
        <p className="muted">Welcome, {auth.name || 'Admin'}. Manage campus information, users, and system settings.</p>
      </header>

      <div className="grid features" style={{ marginTop: '2rem' }}>
        {/* Stats Card 1 */}
        <article className="card">
          <h2 className="h2">👥 User Management</h2>
          <div style={{ marginTop: '1rem' }}>
            <p style={{ fontSize: '2.5rem', fontWeight: '700', color: 'var(--cardinal)', margin: '0' }}>
              {stats.totalUsers.toLocaleString()}
            </p>
            <p className="muted" style={{ marginTop: '0.5rem' }}>Total Registered Users</p>
          </div>
          <p className="muted" style={{ marginTop: '1rem', fontSize: '0.9rem' }}>
            Manage user accounts, roles, and permissions. View user activity and engagement metrics.
          </p>
        </article>

        {/* Stats Card 2 */}
        <article className="card" style={{ cursor: 'pointer' }} onClick={() => navigate('/admin/buildings')}>
          <h2 className="h2">🏛️ Building Management</h2>
          <div style={{ marginTop: '1rem' }}>
            <p style={{ fontSize: '2.5rem', fontWeight: '700', color: 'var(--cardinal)', margin: '0' }}>
              {stats.totalBuildings}
            </p>
            <p className="muted" style={{ marginTop: '0.5rem' }}>Campus Buildings</p>
          </div>
          <p className="muted" style={{ marginTop: '1rem', fontSize: '0.9rem' }}>
            Add, edit, or remove building information. Click to manage buildings.
          </p>
          <Link to="/admin/buildings" className="btn primary" style={{ marginTop: '1rem', display: 'inline-block' }} onClick={(e) => e.stopPropagation()}>
            Manage Buildings →
          </Link>
        </article>

        {/* Stats Card 3 */}
        <article className="card">
          <h2 className="h2">🎯 Active Tours</h2>
          <div style={{ marginTop: '1rem' }}>
            <p style={{ fontSize: '2.5rem', fontWeight: '700', color: 'var(--cardinal)', margin: '0' }}>
              {stats.activeTours}
            </p>
            <p className="muted" style={{ marginTop: '0.5rem' }}>Scheduled Campus Tours</p>
          </div>
          <p className="muted" style={{ marginTop: '1rem', fontSize: '0.9rem' }}>
            Monitor and manage scheduled campus tours. View tour registrations and manage tour guides.
          </p>
        </article>
      </div>

      {/* Admin Actions Section */}
      <div className="card" style={{ marginTop: '2rem', marginBottom: '2rem' }}>
        <h2 className="h2">Admin Capabilities</h2>
        <div style={{ marginTop: '1rem' }}>
          <p className="muted" style={{ marginBottom: '1rem' }}>
            As an administrator, you have access to the following management features:
          </p>
          <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem' }}>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>Manage building data via <code>/api/buildings</code> endpoint (CRUD operations)</span>
            </li>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>View and manage user accounts and roles</span>
            </li>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>Monitor system health and API endpoints</span>
            </li>
            <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
              <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>✓</span>
              <span>Configure campus tour schedules and availability</span>
            </li>
          </ul>
        </div>
      </div>

      {/* Backend Integration Note */}
      <div className="card" style={{ marginTop: '2rem', marginBottom: '2rem', background: 'linear-gradient(135deg, rgba(200,16,46,0.05) 0%, rgba(241,190,72,0.05) 100%)' }}>
        <h2 className="h2">Backend Integration</h2>
        <p className="muted" style={{ marginTop: '1rem' }}>
          This dashboard connects to the backend API running on <code>http://localhost:3000</code>. 
          The following endpoints are available for future CRUD operations:
        </p>
        <div style={{ marginTop: '1rem', display: 'grid', gap: '0.5rem', fontFamily: 'monospace', fontSize: '0.9rem' }}>
          <div><code>GET /api/health</code> - System health check</div>
          <div><code>GET /api/buildings</code> - Retrieve all buildings</div>
          <div><code>POST /api/buildings</code> - Create new building (future)</div>
          <div><code>PUT /api/buildings/:id</code> - Update building (future)</div>
          <div><code>DELETE /api/buildings/:id</code> - Delete building (future)</div>
        </div>
      </div>
    </section>
  )
}

