// Home.jsx

import { useNavigate } from 'react-router-dom'
import { useState, useMemo } from 'react'
import CategoryRow from '../components/CategoryRow'
import data from '../data/courses.json'

export default function Home() {
    const navigate = useNavigate()
    const categories = data.categories || []

    const allCourses = useMemo(
        () => categories.flatMap((c) => c.courses || []),
        [categories]
    )

    const [featured, setFeatured] = useState(allCourses[0])

    const goToCourseDetails = () => {
        if (!featured) return
        navigate(`/course/${featured.id}`, { state: { course: featured } })
    }

    return (
        <div className="text-white">
            {/* Hero Section */}
            {featured && (
                <section className="relative h-[55vh] md:h-[70vh] w-full bg-black mb-6">
                    <img
                        src={featured.thumbnail}
                        alt={featured.title}
                        className="absolute inset-0 w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-black/80 to-black/10" />

                    <div className="relative h-full flex flex-col justify-center max-w-5xl px-6 md:px-16 space-y-4">
                        <p className="text-sm md:text-base text-zinc-200 uppercase tracking-wide">
                            Featured Course
                        </p>
                        <h1 className="text-4xl md:text-6xl font-black drop-shadow-[0_4px_12px_rgba(0,0,0,0.7)]">
                            {featured.title}
                        </h1>
                        <p className="max-w-xl text-sm md:text-base text-zinc-200 line-clamp-3">
                            {featured.description}
                        </p>

                        {/* quick details */}
                        <div className="flex flex-wrap gap-3 text-xs md:text-sm text-zinc-200">
                            <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10">
                                Level: Intermediate
                            </span>
                            <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10">
                                Duration: {featured.duration}
                            </span>
                            <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10">
                                Certificate of Completion
                            </span>
                        </div>

                        <div className="flex">
                            <button
                                onClick={goToCourseDetails}
                                className="inline-flex items-center justify-center gap-2
                  bg-red-600 hover:bg-red-500 text-white font-semibold
                  px-20 md:px-40 py-3 rounded-md
                  text-sm md:text-lg tracking-wide
                  shadow-[0_10px_30px_rgba(0,0,0,0.6)]
                  transition-transform duration-200 hover:scale-[1.03]"
                            >
                                Enroll Now
                            </button>
                        </div>
                    </div>
                </section>
            )}

            {/* Hovering updates the hero */}
            <div
                id="browse"
                className="relative -mt-16 md:-mt-24 space-y-10 max-w-7xl mx-auto px-6 pb-16"
            >
                {categories.map((c) => (
                    <CategoryRow
                        key={c.name}
                        title={c.name}
                        courses={c.courses}
                        onCourseHover={setFeatured}
                    />
                ))}
            </div>
        </div>
    )
}