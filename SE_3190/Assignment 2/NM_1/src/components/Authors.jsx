// Authors.jsx

import { Link } from 'react-router-dom'

export default function Authors() {
  return (
    <div className="max-w-5xl mx-auto">
      <header className="text-center mb-8">
        <h1 className="text-3xl font-bold mb-1">Meet the Authors</h1>
        <p className="opacity-80">A Quick Intro To The Folks Behind SKILLFLIX</p>
      </header>

      <section className="grid sm:grid-cols-2 gap-6">
        {/* Left: Mekhi */}
        <article className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
          <img src="/images/mekhi.jpg" alt="Mekhi San" className="w-full object-cover" />
          <div className="p-4 space-y-2">
            <h2 className="text-xl font-semibold">Mekhi San</h2>
            <p className="text-sm opacity-80">
              Email: <a className="underline decoration-dotted" href="mailto:sanm20@iastate.edu">sanm20@iastate.edu</a>
            </p>
            <p className="text-sm opacity-80">
              Software Engineering student at Iowa State University focused on practical, user‑first projects.
            </p>
          </div>
        </article>

        {/* Right: Ash */}
        <article className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
          <img src="/images/ash.jpeg" alt="Ash Bhuiyan" className="w-full object-cover" />
          <div className="p-4 space-y-2">
            <h2 className="text-xl font-semibold">Ash Bhuiyan</h2>
            <p className="text-sm opacity-80">
              Email: <a className="underline decoration-dotted" href="mailto:mbhuiyan@iastate.edu">mbhuiyan@iastate.edu</a>
            </p>
            <p className="text-sm opacity-80">
              Software Engineering student who loves clean UI and making apps easy to use.
            </p>
          </div>
        </article>
      </section>

      <div className="text-center mt-8">
        <Link to="/faq" className="inline-block bg-gray-800 hover:bg-gray-700 rounded-md px-4 py-2 text-sm">
          Read our FAQ
        </Link>
      </div>
    </div>
  )
}