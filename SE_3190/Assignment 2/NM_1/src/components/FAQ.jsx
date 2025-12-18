// FAQ.jsx

export default function FAQ() {
  const faqs = [
    { q: "What is this site for?", a: "It’s a simple course catalog SPA for SE/COM S 3190 demonstrating routing, forms, validation, and Tailwind styling." },
    { q: "How are courses organized?", a: "Courses are grouped by categories and rendered dynamically from a JSON file." },
    { q: "Who are the authors?", a: "Team NM_1: Mekhi San and Ash Bhuiyan. Both contributed features and design." },
    { q: "What tools were used?", a: "React, React Router, TailwindCSS, and Vite." },
    { q: "What’s coming next?", a: "We’re building out more features for 3090 and 3190 projects—stay tuned!" }
  ]
  return (
    <div className="max-w-4xl mx-auto">
      <header className="text-center mb-8">
        <h1 className="text-3xl font-bold mb-1">Frequently Asked Questions</h1>
        <p className="opacity-80">Quick answers about SkillFlix</p>
      </header>

      <div className="divide-y divide-gray-800 rounded-2xl border border-gray-800 bg-gray-900">
        {faqs.map((item, i) => (
          <details key={i} className="p-4 group">
            <summary className="cursor-pointer font-semibold flex items-center justify-between">
              <span>{item.q}</span>
              <span className="text-sm opacity-70 group-open:rotate-180 transition">⌄</span>
            </summary>
            <p className="mt-2 opacity-80">{item.a}</p>
          </details>
        ))}
      </div>
    </div>
  )
}