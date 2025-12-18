// Navbar.jsx

import Navbar from './components/Navbar.jsx'

import Navbar from './components/Navbar.jsx'
import { Routes, Route } from 'react-router-dom'
import Home from './components/Home.jsx'
import CourseDetails from './components/CourseDetails.jsx'
import EnrollmentForm from './components/EnrollmentForm.jsx'
import PaymentConfirmation from './components/PaymentConfirmation.jsx'
import Authors from './components/Authors.jsx'
import FAQ from './components/FAQ.jsx'

export default function App() {
    return (
        <div className="min-h-screen">
            <Navbar />
            <main className="max-w-7xl mx-auto px-4 py-6">
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/course/:id" element={<CourseDetails />} />
                    <Route path="/enroll" element={<EnrollmentForm />} />
                    <Route path="/payment" element={<PaymentConfirmation />} />
                    <Route path="/authors" element={<Authors />} />
                    <Route path="/faq" element={<FAQ />} />
                </Routes>
            </main>
        </div>
    )
}