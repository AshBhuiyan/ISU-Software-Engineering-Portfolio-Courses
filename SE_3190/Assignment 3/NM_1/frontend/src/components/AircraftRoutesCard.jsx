import { Link } from "react-router-dom";

export default function AircraftRoutesCard({ aircraft, selected, onSelect }) {
  const { _id, serialNumber, model, seatingCapacity, status, assignedRoutes } =
    aircraft;

  return (
    <div
      onClick={() => onSelect?.(aircraft)}
      className={`relative rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 border ${
        selected ? "border-skyvalorAccent" : "border-slate-700"
      } p-4 cursor-pointer hover:-translate-y-1 hover:border-skyvalorAccent hover:shadow-lg hover:shadow-skyvalorAccent/20 transition`}
    >
      {/* Header */}
      <div className="flex justify-between items-center mb-2">
        <span className="text-xs uppercase tracking-wide text-slate-400">
          Aircraft
        </span>
        <span className="text-xs px-2 py-0.5 text-white rounded-full bg-slate-900 border border-slate-600">
          {serialNumber}
        </span>
      </div>

      {/* Basic Info */}
      <h3 className="text-lg text-white font-semibold mb-1">{model}</h3>
      <p className="text-xs text-slate-400 mb-3">
        Seats: {seatingCapacity ?? "N/A"}
      </p>

      {/* Status */}
      <div className="mb-4">
        <span className="block text-[0.7rem] uppercase text-slate-400">
          Status
        </span>
        <span
          className={status === "Active" ? "text-emerald-400" : "text-red-400"}
        >
          {status}
        </span>
      </div>

      {/* Routes Section */}
      <div className="mb-3">
        <span className="block text-[0.7rem] uppercase text-slate-400 mb-2">
          Assigned Routes
        </span>

        <div className="rounded-xl bg-slate-800/50 border border-slate-700 divide-y divide-slate-700">
          {(assignedRoutes || []).map((r, idx) => (
            <div key={idx} className="py-2 px-3 text-sm text-slate-200">
              {r.item}
            </div>
          ))}
        </div>

        {(!assignedRoutes || assignedRoutes.length === 0) && (
          <p className="text-xs text-slate-500">No routes assigned</p>
        )}
      </div>

      {/* View Button */}
      <div className="text-right">
        <Link
          to={`/aircraft/${_id}`}
          onClick={(e) => e.stopPropagation()}
          className="text-xs px-3 py-2 text-white hover:text-black rounded-full bg-skyvalorAccent text-slate-950 font-medium hover:bg-sky-300"
        >
          View Details
        </Link>
      </div>
    </div>
  );
}
