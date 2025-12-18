// PaymentConfirmation.jsx

import { useLocation, useNavigate } from 'react-router-dom'
import { useMemo, useState } from 'react'

function randomTxn() {
    const s = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    let out = ''
    for (let i = 0; i < 12; i++) { out += s[Math.floor(Math.random() * s.length)] }
    return out
}

export default function Payment() {
    const { state } = useLocation()
    const navigate = useNavigate()
    const { course, user, enroll } = state || {}
    const [paid, setPaid] = useState(false)
    const [txn, setTxn] = useState('')

    const [card, setCard] = useState({ name: '', number: '', expiry: '', cvv: '' })
    const [addr, setAddr] = useState({ line1: '', city: '', state: '', zip: '' })
    const [errors, setErrors] = useState({})

    const summary = useMemo(() => ({
        courseTitle: course?.title ?? '—',
        instructor: course?.instructor ?? '—',
        price: course?.price ?? 0,
        userName: user?.name ?? '—',
        userEmail: user?.email ?? '—',
        startDate: enroll?.startDate ?? '—',
        mode: enroll?.mode ?? '—',
    }), [course, user, enroll])

    const validate = () => {
        const e = {}
        if (!card.name.trim()) e.name = 'Cardholder name required'
        if (!/^\d{16}$/.test(card.number.replace(/\s+/g, ''))) e.number = '16-digit card number required'
        if (!/^\d{2}\/\d{2}$/.test(card.expiry)) e.expiry = 'MM/YY format'
        if (!/^\d{3,4}$/.test(card.cvv)) e.cvv = '3-4 digit CVV'
        if (!addr.line1.trim()) e.line1 = 'Address required'
        if (!addr.city.trim()) e.city = 'City required'
        if (!addr.state.trim()) e.state = 'State required'
        if (!/^\d{5}$/.test(addr.zip)) e.zip = '5-digit ZIP'
        setErrors(e); return Object.keys(e).length === 0
    }

    const onPay = (e) => {
        e.preventDefault()
        if (!validate()) return
        setPaid(true)
        setTxn(randomTxn())
    }

    if (!course || !user || !enroll) {
        return <p className="text-red-400">Missing context. Please start from enrollment.</p>
    }

    return (
        <div className="grid lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2">
                <form onSubmit={onPay} className="space-y-6">
                    <fieldset className="border border-gray-800 rounded-lg p-4">
                        <legend className="px-2 text-sm uppercase tracking-wide opacity-80">Payment Details</legend>
                        <div className="grid sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm mb-1">Cardholder Name</label>
                                <input className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={card.name} onChange={e => setCard({ ...card, name: e.target.value })} />
                                {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name}</p>}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">Card Number</label>
                                <input inputMode="numeric" maxLength={19} placeholder="1234123412341234"
                                    className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={card.number} onChange={e => setCard({ ...card, number: e.target.value })} />
                                {errors.number && <p className="text-red-400 text-xs mt-1">{errors.number}</p>}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">Expiry (MM/YY)</label>
                                <input placeholder="08/27" className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={card.expiry} onChange={e => setCard({ ...card, expiry: e.target.value })} />
                                {errors.expiry && <p className="text-red-400 text-xs mt-1">{errors.expiry}</p>}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">CVV</label>
                                <input inputMode="numeric" maxLength={4} className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={card.cvv} onChange={e => setCard({ ...card, cvv: e.target.value })} />
                                {errors.cvv && <p className="text-red-400 text-xs mt-1">{errors.cvv}</p>}
                            </div>
                        </div>
                    </fieldset>

                    <fieldset className="border border-gray-800 rounded-lg p-4">
                        <legend className="px-2 text-sm uppercase tracking-wide opacity-80">Billing & Shipping</legend>
                        <div className="grid sm:grid-cols-2 gap-4">
                            <div className="sm:col-span-2">
                                <label className="block text-sm mb-1">Address Line 1</label>
                                <input className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={addr.line1} onChange={e => setAddr({ ...addr, line1: e.target.value })} />
                                {errors.line1 && <p className="text-red-400 text-xs mt-1">{errors.line1}</p>}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">City</label>
                                <input className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={addr.city} onChange={e => setAddr({ ...addr, city: e.target.value })} />
                                {errors.city && <p className="text-red-400 text-xs mt-1">{errors.city}</p>}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">State</label>
                                <input className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={addr.state} onChange={e => setAddr({ ...addr, state: e.target.value })} />
                                {errors.state && <p className="text-red-400 text-xs mt-1">{errors.state}</p>}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">ZIP</label>
                                <input inputMode="numeric" maxLength={5} className="w-full bg-gray-900 border border-gray-800 rounded-md px-3 py-2"
                                    value={addr.zip} onChange={e => setAddr({ ...addr, zip: e.target.value })} />
                                {errors.zip && <p className="text-red-400 text-xs mt-1">{errors.zip}</p>}
                            </div>
                        </div>
                    </fieldset>

                    <div className="flex justify-end">
                        <button type="submit" className="bg-pink-600 hover:bg-pink-500 rounded-md px-5 py-2.5 font-semibold">
                            Confirm Payment
                        </button>
                    </div>
                </form>
            </div>

            <aside className="space-y-4">
                <div className="border border-gray-800 rounded-lg p-4">
                    <h3 className="font-semibold mb-2">Review</h3>
                    <div className="text-sm space-y-1">
                        <div className="flex justify-between"><span>Course</span><span>{summary.courseTitle}</span></div>
                        <div className="flex justify-between"><span>Instructor</span><span>{summary.instructor}</span></div>
                        <div className="flex justify-between"><span>Price</span><span>${summary.price?.toFixed(2)}</span></div>
                        <div className="flex justify-between"><span>Name</span><span>{summary.userName}</span></div>
                        <div className="flex justify-between"><span>Email</span><span>{summary.userEmail}</span></div>
                        <div className="flex justify-between"><span>Start</span><span>{summary.startDate}</span></div>
                        <div className="flex justify-between"><span>Mode</span><span>{summary.mode}</span></div>
                    </div>
                </div>

                {paid && (
                    <div className="border border-emerald-700 bg-emerald-950/40 rounded-lg p-4">
                        <div className="text-emerald-400 font-semibold mb-1">Payment Successful</div>
                        <div className="text-sm">Transaction ID: <span className="font-mono">{txn}</span></div>
                        <button className="mt-4 bg-gray-800 hover:bg-gray-700 rounded-md px-3 py-2"
                            onClick={() => navigate('/', { replace: true })}>
                            Return to Home
                        </button>
                    </div>
                )}
            </aside>
        </div>
    )
}