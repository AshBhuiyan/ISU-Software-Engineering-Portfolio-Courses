import React from 'react'
import { Link } from 'react-router-dom'

export default function About() {
  // Team members
  const teamMembers = [
    {
      name: 'Mekhi San',
      email: 'msan@iastate.edu',
      role: 'Full Stack Developer',
      image: '/assets/images/Mekhi.jpg'
    },
    {
      name: 'Ash Bhuiyan',
      email: 'abhuiyan@iastate.edu',
      role: 'Full Stack Developer',
      image: '/assets/images/ash.jpeg'
    }
  ]

  return (
    <section className="campus">
      <header className="campus-header">
        <h1 className="display">About ISU Campus Explorer</h1>
        <p className="muted">Learn more about our project and team.</p>
      </header>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">Project Overview</h2>
        <p className="muted" style={{ marginTop: '1rem', lineHeight: '1.8', fontSize: '1.05rem' }}>
          ISU Campus Explorer is a comprehensive web application designed to help students, faculty, and visitors 
          navigate and explore the Iowa State University campus. The platform features an interactive campus map, 
          detailed building information, and a custom tour planning system that allows users to create personalized 
          campus tours. Built with modern web technologies including React, Node.js, Express, and MongoDB, the 
          application provides an intuitive and accessible way to discover campus facilities, departments, and points 
          of interest.
        </p>
      </div>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">Course Information</h2>
        <div style={{ marginTop: '1rem' }}>
          <p style={{ margin: '0.5rem 0', fontSize: '1.05rem' }}>
            <strong>Course:</strong> SE/COM S 319
          </p>
          <p style={{ margin: '0.5rem 0', fontSize: '1.05rem' }}>
            <strong>Semester:</strong> Fall 2025
          </p>
          <p style={{ margin: '0.5rem 0', fontSize: '1.05rem' }}>
            <strong>Project:</strong> Final Project - ISU Campus Explorer
          </p>
        </div>
      </div>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">Team Members</h2>
        <p className="muted" style={{ marginTop: '0.5rem', marginBottom: '1.5rem' }}>
          Meet the team behind ISU Campus Explorer
        </p>
        <div style={{ 
          display: 'flex', 
          flexDirection: 'row',
          gap: '2rem', 
          marginTop: '1rem',
          flexWrap: 'wrap',
          justifyContent: 'center',
          alignItems: 'flex-start'
        }}
        className="team-members-container"
        >
          {teamMembers.map((member, index) => (
            <div 
              key={index}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                padding: '1.5rem',
                background: 'rgba(200,16,46,0.05)',
                borderRadius: '0.75rem',
                border: '1px solid rgba(200,16,46,0.2)',
                flex: '1',
                minWidth: '280px',
                maxWidth: '350px'
              }}
            >
              {member.image && (
                <img
                  src={member.image}
                  alt={member.name}
                  style={{
                    width: '250px',
                    height: '250px',
                    objectFit: 'cover',
                    objectPosition: member.name === 'Ash Bhuiyan' ? 'top' : 'center',
                    borderRadius: '12px',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    marginBottom: '1.5rem'
                  }}
                />
              )}
              <h3 style={{ margin: 0, color: 'var(--cardinal)', fontSize: '1.25rem', textAlign: 'center' }}>
                {member.name}
              </h3>
              <p style={{ margin: '0.5rem 0 0 0', color: 'var(--text-muted)', textAlign: 'center' }}>
                <a 
                  href={`mailto:${member.email}`}
                  style={{ color: 'var(--cardinal)', textDecoration: 'none' }}
                >
                  {member.email}
                </a>
              </p>
              {member.role && (
                <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.9rem', color: 'var(--text)', textAlign: 'center' }}>
                  {member.role}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">Key Features</h2>
        <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem', marginTop: '1rem' }}>
          <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
            <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>🗺️</span>
            <span><strong>Interactive Campus Map:</strong> Explore buildings with an intuitive visual map interface</span>
          </li>
          <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
            <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>🏛️</span>
            <span><strong>Building Directory:</strong> Comprehensive information about campus facilities</span>
          </li>
          <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
            <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>🎯</span>
            <span><strong>Custom Tour Planning:</strong> Create and save personalized campus tours</span>
          </li>
          <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
            <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>👤</span>
            <span><strong>User Accounts:</strong> Save preferences and manage your tours</span>
          </li>
          <li style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
            <span style={{ color: 'var(--cardinal)', fontSize: '1.2rem' }}>♿</span>
            <span><strong>Accessibility:</strong> Designed with universal access in mind</span>
          </li>
        </ul>
      </div>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h2 className="h2">Technology Stack</h2>
        <div style={{ marginTop: '1rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          <div>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>Frontend</h3>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              <li className="muted">React</li>
              <li className="muted">Vite</li>
              <li className="muted">React Router</li>
            </ul>
          </div>
          <div>
            <h3 className="h3" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>Backend</h3>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              <li className="muted">Node.js</li>
              <li className="muted">Express</li>
              <li className="muted">MongoDB</li>
              <li className="muted">Mongoose</li>
            </ul>
          </div>
        </div>
      </div>

      <div style={{ marginTop: '2rem', textAlign: 'center' }}>
        <Link to="/" className="btn primary">
          Return to Home
        </Link>
      </div>
    </section>
  )
}
