// CategoryRow.jsx

import CourseCard from './CourseCard'

export default function CategoryRow({ title, courses, onCourseHover }) {
  return (
    <section className="mb-2">
      <h2 className="text-lg md:text-2xl font-semibold mb-3 px-1">
        {title}
      </h2>

      <div className="group relative">
        <div
          className="flex justify-center gap-5 overflow-x-auto scrollbar-hide pb-6"
        >
          {courses.map((course) => (
            <CourseCard
              key={course.id}
              course={course}
              onHover={onCourseHover}
            />
          ))}
        </div>

        <div className="pointer-events-none absolute inset-y-0 right-0 w-24 bg-gradient-to-l from-black to-transparent" />
      </div>
    </section>
  )
}