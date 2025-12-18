import { Link } from "react-router-dom";

export default function AircraftCard({ aircraft, selected, onSelect }) {
  const {
    _id,
    serialNumber,
    airline,
    model,
    seatingCapacity,
    status,
    assignedRouteId,
  } = aircraft;

  return (
    <div
      onClick={() => onSelect?.(aircraft)}
      className={`relative rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 border ${
        selected ? "border-skyvalorAccent" : "border-slate-700"
      } p-4 cursor-pointer hover:-translate-y-1 hover:border-skyvalorAccent hover:shadow-lg hover:shadow-skyvalorAccent/20 transition`}
    >
      <div className="flex justify-between items-center mb-2">
        <span className="text-xs uppercase tracking-wide text-slate-400">
          Aircraft
        </span>
        <span className="text-xs px-2 py-0.5 text-white rounded-full bg-slate-900 border border-slate-600">
          {serialNumber}
        </span>
      </div>

      <div className="flex justify-between items-center mb-2">
        <h3 className="text-lg text-white font-semibold mb-1">{model}</h3>
        <span className="text-xs px-2 py-0.5 text-white rounded-full bg-slate-900 border border-slate-600">
          {airline == "Delta"
            ? "Delta Air Lines"
            : airline == "Etihad"
            ? "Etihad Airways"
            : airline == "United"
            ? "United Airlines"
            : airline == "American"
            ? "American Airlines"
            : airline}
        </span>
      </div>

      <p className="text-xs text-slate-400 mb-3">
        Seats: {seatingCapacity ?? "N/A"}
      </p>

      <div className="flex justify-between items-center text-xs text-slate-300 mb-3">
        <div>
          <span className="block text-slate-400 text-[0.7rem] uppercase">
            Status
          </span>
          <span
            className={
              status === "Active"
                ? "text-emerald-400"
                : status === "In Maintenance"
                ? "text-amber-300"
                : "text-slate-200"
            }
          >
            {status || "Scheduled"}
          </span>
        </div>
        <div className="text-right">
          <Link
            to={`/aircraft/${_id}`}
            onClick={(e) => e.stopPropagation()}
            className="text-xs px-3 py-3 text-white hover:text-black rounded-full bg-skyvalorAccent text-slate-950 font-medium hover:bg-sky-300"
          >
            View Details
          </Link>
        </div>
      </div>
    </div>
  );
}
