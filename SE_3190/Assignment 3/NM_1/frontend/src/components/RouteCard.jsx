import { Link } from "react-router-dom";

export default function RouteCard({ route, selected, onSelect }) {
  const { _id, id, displayId, airline, aircraft, from, to } = route;

  return (
    <div
      onClick={() => onSelect?.(route)}
      className={`relative rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 border ${
        selected ? "border-skyvalorAccent" : "border-slate-700"
      } p-4 cursor-pointer hover:-translate-y-1 hover:border-skyvalorAccent hover:shadow-lg hover:shadow-skyvalorAccent/20 transition`}
    >
      <div className="flex justify-between items-center mb-2">
        <span className="text-xs uppercase tracking-wide text-slate-400">
          Route
        </span>
        <span className="text-xs px-2 py-0.5 rounded-full text-white bg-slate-900 border border-slate-600">
          {displayId}
        </span>
      </div>

      <div className="flex items-center justify-between gap-4 mb-3">
        <div className="flex flex-col">
          <span className="text-2xl text-white font-semibold">
            {from.code} <span className="text-slate-500 text-base">&rarr;</span>{" "}
            {to.code}
          </span>
          <span className="w-fit mt-5 text-xs px-2 py-0.5 rounded-full text-white bg-slate-900 border border-slate-600">
            {aircraft}
          </span>
        </div>
      </div>

      <div className="text-right">
        <Link
          to={`/routes/${_id}`}
          onClick={(e) => e.stopPropagation()}
          className="text-xs px-3 py-3 mb-15 text-white hover:text-black rounded-full bg-skyvalorAccent text-slate-950 font-medium hover:bg-sky-300"
        >
          View Details
        </Link>
      </div>
    </div>
  );
}
