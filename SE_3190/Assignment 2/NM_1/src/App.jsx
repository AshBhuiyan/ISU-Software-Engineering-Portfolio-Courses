// App.jsx

import { Routes, Route, Link, useLocation, useNavigate } from 'react-router-dom'
import Home from './components/Home.jsx'
import CourseDetails from './components/CourseDetails.jsx'
import EnrollmentForm from './components/EnrollmentForm.jsx'
import PaymentConfirmation from './components/PaymentConfirmation.jsx'
import Authors from './components/Authors.jsx'
import FAQ from './components/FAQ.jsx'

function TopNav() {
    const { pathname } = useLocation()
    const navigate = useNavigate()

    const linkCls = (p) =>
        'text-sm font-medium transition ' +
        (pathname === p ? 'text-white' : 'text-zinc-300 hover:text-white')

    const handleHomeClick = () => {
        if (pathname !== '/') {
            navigate('/')
        }
        window.scrollTo({ top: 0, left: 0, behavior: 'smooth' })
    }

    const scrollToCourses = () => {
        const target = document.getElementById('browse')
        if (target) {
            target.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
    }

    const handleCoursesClick = (e) => {
        e.preventDefault()

        if (pathname !== '/') {
            navigate('/')
            // wait for Home to render, then scroll to tiles
            setTimeout(scrollToCourses, 150)
        } else {
            scrollToCourses()
        }
    }

    return (
        <nav className="ml-8">
            <ul className="flex items-center gap-6">
                <li>
                    <Link
                        to="/"
                        onClick={handleHomeClick}
                        className={linkCls('/')}
                    >
                        Home
                    </Link>
                </li>
                <li>
                    {/* scroll to the browse section */}
                    <a
                        href="#browse"
                        onClick={handleCoursesClick}
                        className="text-sm font-medium text-zinc-300 hover:text-white transition"
                    >
                        Courses
                    </a>
                </li>
                <li>
                    <Link to="/authors" className={linkCls('/authors')}>
                        Authors
                    </Link>
                </li>
                <li>
                    <Link to="/faq" className={linkCls('/faq')}>
                        FAQ
                    </Link>
                </li>
            </ul>
        </nav>
    )
}

export default function App() {
    return (
        <div className="min-h-screen bg-black text-white">
            {/* Netflix-style sticky top bar */}
            <header className="sticky top-0 z-50 bg-black/80 backdrop-blur border-b border-zinc-900">
                <div className="flex items-center justify-between px-6 md:px-10 py-3">
                    <div className="flex items-center gap-8">
                        <Link to="/" className="text-3xl font-black tracking-tight text-red-600">
                            SKILLFLIX
                        </Link>
                        <TopNav />
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="hidden md:flex flex-col text-right text-[11px] leading-tight text-zinc-300">
                            <span className="font-semibold text-zinc-100">
                                Assignment 2 Portal
                            </span>
                            <span className="text-zinc-400">
                                Browse courses &amp; simulate signup
                            </span>
                        </div>
                        <button className="w-8 h-8 rounded bg-zinc-800 text-xs font-semibold flex items-center justify-center">
                            NM_1
                        </button>
                    </div>
                </div>
            </header>

            <main className="pb-10 bg-gradient-to-b from-zinc-950 via-black to-black">
                <div className="w-full">
                    <Routes>
                        <Route path="/" element={<Home />} />
                        <Route path="/course/:id" element={<CourseDetails />} />
                        <Route path="/enroll" element={<EnrollmentForm />} />
                        <Route path="/payment" element={<PaymentConfirmation />} />
                        <Route path="/authors" element={<Authors />} />
                        <Route path="/faq" element={<FAQ />} />
                    </Routes>
                </div>
            </main>

            <footer className="border-t border-zinc-900 py-6 text-center text-[10px] text-zinc-500">
                &copy; {new Date().getFullYear()} SKILLFKIX (Ash & Mekhi)
            </footer>
        </div>
    )
}