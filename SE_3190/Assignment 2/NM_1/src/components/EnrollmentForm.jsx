// EnrollmentForm.jsx
// Mekhi San
// 810968272

import { useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'

export default function Enrollment(){
  const { state } = useLocation()
  const navigate = useNavigate()
  const course = state?.course

  const [user, setUser] = useState({ name: '', email: '' })
  const [enroll, setEnroll] = useState({ startDate: '', mode: 'Online', note: '' })
  const [errors, setErrors] = useState({})

  const validate = () => {
    const e = {}
    if(!user.name.trim()) e.name = 'Full name is required'
    if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.email)) e.email = 'Valid email is required'
    if(!enroll.startDate) e.startDate = 'Pick a start date'
    if(!enroll.mode) e.mode = 'Choose a mode'
    setErrors(e); return Object.keys(e).length === 0
  }

  const onSubmit = (ev) => {
    ev.preventDefault()
    if(!validate()) return
    navigate('/payment', { state: { course, user, enroll } })
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-2">Enrollment</h1>
      {course && <p className="mb-6 opacity-80">Course: <span className="font-medium">{course.title}</span></p>}

      <form onSubmit={onSubmit} className="space-y-8">
        <fieldset className="border border-gray-800 rounded-lg p-4">
          <legend className="px-2 text-sm uppercase tracking-wide opacity-80">User Information</legend>
          <div className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-1">Full Name</label>
              <input className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                value={user.name} onChange={e => setUser({...user, name: e.target.value})} />
              {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name}</p>}
            </div>
            <div>
              <label className="block text-sm mb-1">Email</label>
              <input type="email" className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                value={user.email} onChange={e => setUser({...user, email: e.target.value})} />
              {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email}</p>}
            </div>
          </div>
        </fieldset>

        <fieldset className="border border-gray-800 rounded-lg p-4">
          <legend className="px-2 text-sm uppercase tracking-wide opacity-80">Enrollment Details</legend>
          <div className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-1">Preferred Start Date</label>
              <input type="date" className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                value={enroll.startDate} onChange={e => setEnroll({...enroll, startDate: e.target.value})} />
              {errors.startDate && <p className="text-red-400 text-xs mt-1">{errors.startDate}</p>}
            </div>
            <div>
              <label className="block text-sm mb-1">Mode</label>
              <select className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                value={enroll.mode} onChange={e => setEnroll({...enroll, mode: e.target.value})}>
                <option>Online</option>
                <option>In-Person</option>
              </select>
              {errors.mode && <p className="text-red-400 text-xs mt-1">{errors.mode}</p>}
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm mb-1">Message to Instructor (optional)</label>
              <textarea rows="4" className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                value={enroll.note} onChange={e => setEnroll({...enroll, note: e.target.value})} />
            </div>
          </div>
        </fieldset>

        <div className="flex justify-end">
          <button type="submit" className="bg-pink-600 hover:bg-pink-500 rounded-md px-5 py-2.5 font-semibold">
            Continue to Payment
          </button>
        </div>
      </form>
    </div>
  )
}
