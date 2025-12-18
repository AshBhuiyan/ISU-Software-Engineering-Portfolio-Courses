// CourseDetails.jsx

import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import data from '../data/courses.json'

export default function CourseDetails() {
  const { state } = useLocation()
  const { id } = useParams()
  const navigate = useNavigate()

  const [course, setCourse] = useState(state?.course || null)

  useEffect(() => {
    if (!course) {
      const all = data.categories.flatMap((c) => c.courses || [])
      const found = all.find((c) => String(c.id) === String(id))
      if (found) setCourse(found)
    }
  }, [course, id])

  if (!course) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center text-white">
        Course not found.
      </div>
    )
  }

  const pseudoModules = [
    'Foundations & core concepts',
    'Hands-on projects and real-world examples',
    'Best practices, patterns, and debugging',
    'Capstone project & review',
  ]

  return (
    <div className="min-h-screen bg-gradient-to-b from-zinc-950 to-black text-white">
      <div className="max-w-5xl mx-auto px-4 py-8 md:py-12">
        <button
          onClick={() => navigate(-1)}
          className="text-sm text-zinc-300 mb-4 hover:text-white"
        >
          ← Back to Home
        </button>

        <div className="flex flex-col md:flex-row gap-6">
          <div className="md:w-2/5">
            <img
              src={course.thumbnail}
              alt={course.title}
              className="w-full rounded-md shadow-lg object-cover"
            />
          </div>

          <div className="md:w-3/5 space-y-3">
            <h1 className="text-3xl md:text-4xl font-bold">
              {course.title}
            </h1>
            <p className="text-zinc-300">{course.description}</p>

            <div className="flex flex-wrap gap-2 text-xs">
              <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10">
                Duration: {course.duration}
              </span>
              <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10">
                Level: Intermediate
              </span>
              <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10">
                Price: ${course.price.toFixed(2)}
              </span>
            </div>

            <div className="mt-4">
              <h2 className="font-semibold mb-1">You’ll learn how to:</h2>
              <ul className="list-disc list-inside text-sm text-zinc-200 space-y-1">
                <li>Apply {course.title} in realistic, project-based scenarios.</li>
                <li>Understand common pitfalls and how to avoid them.</li>
                <li>Build confidence through guided exercises.</li>
                <li>Prepare for real-world work using these skills.</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="mt-8 grid md:grid-cols-2 gap-8">
          <div>
            <h2 className="text-xl font-semibold mb-2">Course Outline</h2>
            <ol className="list-decimal list-inside text-sm text-zinc-200 space-y-1">
              {pseudoModules.map((m, idx) => (
                <li key={idx}>{m}</li>
              ))}
            </ol>
          </div>

          <div>
            <h2 className="text-xl font-semibold mb-2">Who is this for?</h2>
            <p className="text-sm text-zinc-200">
              This course is ideal for learners who have basic programming
              experience and want a focused, hands-on path into {course.title}.
              No prior professional experience is required.
            </p>
          </div>
        </div>

        <div className="mt-10 flex flex-wrap gap-3">
          <button
            onClick={() => navigate('/enroll', { state: { course } })}
            className="bg-red-600 hover:bg-red-500 px-6 py-2 rounded-md text-sm font-semibold"
          >
            Continue to Enrollment
          </button>
          <button
            onClick={() => navigate('/')}
            className="border border-zinc-600 px-6 py-2 rounded-md text-sm text-zinc-200 hover:bg-zinc-800"
          >
            Keep Browsing
          </button>
        </div>
      </div>
    </div>
  )
}