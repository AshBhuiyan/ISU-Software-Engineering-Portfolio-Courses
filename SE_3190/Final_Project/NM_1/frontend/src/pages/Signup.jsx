import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function Signup({ auth, onLogin }){
  const navigate = useNavigate()
  const [form,setForm]=useState({name:'',email:'',password:'',confirm:'',terms:false})
  const [errors,setErrors]=useState({})
  const [serverError, setServerError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

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
              <Link to="/admin" className="btn primary">Go to Admin Dashboard</Link>
            ) : (
              <Link to="/student" className="btn primary">Go to Student Dashboard</Link>
            )}
            <Link to="/" className="btn secondary">Return to Home</Link>
          </div>
        </div>
      </section>
    )
  }

  function validate(){
    const e={}
    if(!form.name.trim()) e.name='Please enter your full name.'
    if(!/\S+@\S+\.\S+/.test(form.email)) e.email='Please enter a valid email address.'
    if(form.password.length<8) e.password='Password must be at least 8 characters long.'
    if(form.password!==form.confirm) e.confirm='Passwords do not match.'
    if(!form.terms) e.terms='Please accept the Terms of Service to continue.'
    setErrors(e); return Object.keys(e).length===0
  }

  async function handleSubmit(ev){
    ev.preventDefault()
    if(!validate()) return
    
    setIsSubmitting(true)
    setServerError(null)

    try {
      const response = await fetch('/auth/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: form.name.trim(),
          email: form.email.trim(),
          password: form.password,
        }),
      })

      const data = await response.json()

      if (!response.ok || !data.success) {
        setServerError(data.message || 'Failed to create account')
        setIsSubmitting(false)
        return
      }

      // Auto-login the user
      if (onLogin) {
        onLogin({
          role: data.user.role,
          name: data.user.name,
          email: data.user.email,
        })
      }

      // Redirect based on role
      if (data.user.role === 'admin') {
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
      <div className="card form-card" role="region" aria-labelledby="signup-title">
        <h1 id="signup-title" className="h1">Create Your Account</h1>
        <p className="muted">Join the Iowa State University campus explorer community. Create an account to save your favorite locations and access personalized features.</p>
        <div style={{ 
          background: 'rgba(200,16,46,0.1)', 
          border: '1px solid var(--cardinal)', 
          borderRadius: '0.5rem', 
          padding: '1rem', 
          marginTop: '1rem',
          marginBottom: '1rem'
        }}>
          <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--cardinal)', fontWeight: '600' }}>
            ℹ️ Create your account to access personalized features and save campus tours.
          </p>
          <p style={{ margin: '0.5rem 0 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            New accounts are created with student role by default. Admin access requires system administrator approval.
          </p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="name">Full Name</label>
          <input id="name" name="name" type="text" placeholder="Enter your full name"
            value={form.name} onChange={e=>setForm({...form,name:e.target.value})}
            aria-invalid={!!errors.name} aria-describedby={errors.name?'name-err':undefined} required />
          {errors.name && <p id="name-err" className="error">{errors.name}</p>}

          <label htmlFor="email">ISU Email Address</label>
          <input id="email" name="email" type="email" placeholder="cyclone@iastate.edu"
            value={form.email} onChange={e=>setForm({...form,email:e.target.value})}
            aria-invalid={!!errors.email} aria-describedby={errors.email?'email-err':undefined} required />
          {errors.email && <p id="email-err" className="error">{errors.email}</p>}

          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" placeholder="Create a secure password"
            value={form.password} onChange={e=>setForm({...form,password:e.target.value})}
            aria-invalid={!!errors.password} aria-describedby={errors.password?'password-err':undefined}
            required minLength={8} />
          {errors.password && <p id="password-err" className="error">{errors.password}</p>}

          <label htmlFor="confirm">Confirm Password</label>
          <input id="confirm" name="confirm" type="password" placeholder="Re-enter your password"
            value={form.confirm} onChange={e=>setForm({...form,confirm:e.target.value})}
            aria-invalid={!!errors.confirm} aria-describedby={errors.confirm?'confirm-err':undefined}
            required minLength={8} />
          {errors.confirm && <p id="confirm-err" className="error">{errors.confirm}</p>}

          <div className="checkbox">
            <input id="terms" type="checkbox" checked={form.terms}
              onChange={e=>setForm({...form,terms:e.target.checked})}/>
            <label htmlFor="terms">I agree to the <a href="#" onClick={(e)=>e.preventDefault()} style={{color: 'var(--cardinal)', textDecoration: 'none'}}>Terms of Service</a> and <a href="#" onClick={(e)=>e.preventDefault()} style={{color: 'var(--cardinal)', textDecoration: 'none'}}>Privacy Policy</a>.</label>
          </div>
          {errors.terms && <p className="error">{errors.terms}</p>}

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
            {isSubmitting ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>

        <div style={{textAlign: 'center', marginTop: '2rem', paddingTop: '2rem', borderTop: '1px solid var(--border)'}}>
          <p className="muted">Already have an account? <Link to="/login" style={{color: 'var(--cardinal)', textDecoration: 'none', fontWeight: '600'}}>Sign in here</Link></p>
        </div>
      </div>
    </section>
  )
}
