// CourseCard.jsx

import { useNavigate } from 'react-router-dom'

export default function CourseCard({ course, onHover }) {
  const navigate = useNavigate()
  const go = () =>
    navigate(`/course/${course.id}`, { state: { course } })

  return (
    <button
      onClick={go}
      onMouseEnter={() => onHover && onHover(course)}
      className="relative shrink-0 cursor-pointer
        w-56 sm:w-64 md:w-72
        rounded-md overflow-hidden
        bg-zinc-900
        shadow-[0_10px_25px_rgba(0,0,0,0.7)]
        transition-transform duration-200 ease-out
        hover:scale-110 hover:z-30
        focus:outline-none focus:ring-2 focus:ring-red-600"
    >
      <img
        src={course.thumbnail}
        alt={course.title}
        className="w-full aspect-[16/10] object-cover"
      />

      <div
        className="absolute inset-x-0 bottom-0
        bg-gradient-to-t from-black/90 via-black/70 to-transparent
        p-3 text-xs sm:text-sm"
      >
        <p className="font-semibold line-clamp-2">{course.title}</p>
        <p className="text-zinc-300 mt-1">
          {course.duration} • ${course.price.toFixed(2)}
        </p>
      </div>
    </button>
  )
}