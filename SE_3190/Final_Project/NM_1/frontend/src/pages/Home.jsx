import React from 'react'
import { Link } from 'react-router-dom'

export default function Home(){
  return (
    <section className="home">
      <div className="hero">
        <div className="badge">Iowa State University</div>
        <h1 className="display">Explore Our Campus</h1>
        <p className="lede muted">Discover Iowa State University's beautiful campus with our comprehensive digital map and building directory. Navigate like a Cyclone with confidence and ease.</p>

        <div className="cta-row">
          <Link className="btn primary" to="/campus" aria-label="View Interactive Campus Map">
            Explore Campus Map
          </Link>
          <Link className="btn secondary" to="/signup" aria-label="Create Student Account">
            Get Started
          </Link>
        </div>
      </div>

      <div className="grid features">
        <article className="card">
          <h2 className="h2">🗺️ Interactive Maps</h2>
          <p className="muted">Explore detailed, interactive maps of Iowa State's campus with building locations, pathways, and points of interest.</p>
        </article>
        <article className="card">
          <h2 className="h2">🏛️ Building Directory</h2>
          <p className="muted">Find comprehensive information about academic buildings, residence halls, dining locations, and campus facilities.</p>
        </article>
        <article className="card">
          <h2 className="h2">♿ Accessibility First</h2>
          <p className="muted">Designed with universal access in mind, featuring screen reader support and keyboard navigation throughout.</p>
        </article>
        <article className="card">
          <h2 className="h2">📱 Mobile Optimized</h2>
          <p className="muted">Fully responsive design ensures a seamless experience across all devices, from desktop to mobile.</p>
        </article>
        <article className="card">
          <h2 className="h2">🎨 ISU Branding</h2>
          <p className="muted">Authentic Iowa State University visual identity with official Cardinal and Gold color scheme.</p>
        </article>
        <article className="card">
          <h2 className="h2">⚡ Fast & Reliable</h2>
          <p className="muted">Built with modern web technologies for lightning-fast load times and smooth navigation.</p>
        </article>
      </div>
    </section>
  )
}
