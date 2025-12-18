import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'

export default function Login({ onLogin, auth }){
  const [form, setForm] = useState({ email:'', password:'' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const navigate = useNavigate()

  // If already authenticated, show message
  if (auth.isAuthenticated) {
    return (
      <section className="centered">
        <div className="card form-card" role="region" aria-labelledby="already-logged-in-title">
          <h1 id="already-logged-in-title" className="h1">Already Logged In</h1>
          <p className="muted" style={{ marginBottom: '2rem' }}>
            You are already logged in as {auth.email}.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            {auth.role === 'admin' ? (
              <Link to="/admin" className="btn primary">
                Go to Admin Dashboard
              </Link>
            ) : (
              <Link to="/student" className="btn primary">
                Go to Student Dashboard
              </Link>
            )}
            <Link to="/" className="btn secondary">
              Return to Home
            </Link>
          </div>
        </div>
      </section>
    )
  }

  function validate(){
    const e={}
    if(!/\S+@\S+\.\S+/.test(form.email)) e.email='Please enter a valid email address.'
    if(form.password.length<8) e.password='Password must be at least 8 characters long.'
    setErrors(e)
    return Object.keys(e).length===0
  }

  async function onSubmit(ev){
    ev.preventDefault()
    if(!validate()) return
    
    setIsSubmitting(true)
    setServerError(null)

    try {
      const response = await fetch('/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
        }),
      })

      const data = await response.json()

      if (!response.ok || !data.success) {
        setServerError(data.message || 'Invalid email or password')
        setIsSubmitting(false)
        return
      }

      // Call onLogin with user data
      if (onLogin) {
        onLogin({
          role: data.role,
          name: data.name,
          email: data.email,
        })
      }

      // Redirect based on role
      if (data.role === 'admin') {
        navigate('/admin')
      } else {
        navigate('/student')
      }
    } catch (err) {
      setServerError('Network error. Please check if the backend server is running.')
      setIsSubmitting(false)
    }
  }

  return (
    <section className="centered">
      <div className="card form-card" role="region" aria-labelledby="login-title">
        <h1 id="login-title" className="h1">Sign In to Your Account</h1>
        <p className="muted">Access your Iowa State University campus explorer account to save preferences and access personalized features.</p>
        <p className="muted" style={{ fontSize: '0.85rem', marginTop: '0.5rem', fontStyle: 'italic' }}>
          Note: Admin accounts are pre-created in the backend. Signup is for non-admin users only.
        </p>

        <form onSubmit={onSubmit} noValidate>
          <label htmlFor="email">ISU Email Address</label>
          <input id="email" name="email" type="email" autoComplete="email"
            placeholder="cyclone@iastate.edu" value={form.email}
            onChange={e=>{
              setForm({...form, email:e.target.value})
              setServerError(null)
            }}
            aria-invalid={!!errors.email} aria-describedby={errors.email?'email-err':undefined} required />
          {errors.email && <p id="email-err" className="error">{errors.email}</p>}

          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" autoComplete="current-password"
            placeholder="Enter your password" value={form.password}
            onChange={e=>{
              setForm({...form, password:e.target.value})
              setServerError(null)
            }}
            aria-invalid={!!errors.password} aria-describedby={errors.password?'password-err':undefined}
            required minLength={8} />
          {errors.password && <p id="password-err" className="error">{errors.password}</p>}

          {serverError && (
            <p className="error" role="alert" style={{ marginTop: '1rem' }}>
              {serverError}
            </p>
          )}

          <button 
            type="submit" 
            className="btn primary" 
            style={{marginTop: '1.5rem', width: '100%'}}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div style={{textAlign: 'center', marginTop: '2rem', paddingTop: '2rem', borderTop: '1px solid var(--border)'}}>
          <p className="muted">Don't have an account? <Link to="/signup" style={{color: 'var(--cardinal)', textDecoration: 'none', fontWeight: '600'}}>Create one here</Link></p>
        </div>
      </div>
    </section>
  )
}
