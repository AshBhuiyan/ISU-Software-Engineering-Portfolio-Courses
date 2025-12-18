import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";

import LoadingSpinner from "../components/LoadingSpinner.jsx";
import ErrorAlert from "../components/ErrorAlert.jsx";
import ConfirmDialog from "../components/ConfirmDialog.jsx";

const BASE_URL = "http://localhost:8081/api/aircraft";

// -------------------------------
// Backend API Functions (LOCAL)
// -------------------------------
async function fetchAircraftById(id) {
  const res = await fetch(`${BASE_URL}/${id}`);
  if (!res.ok) throw new Error("Failed to fetch aircraft");
  return res.json();
}

async function deleteAircraftById(id) {
  const res = await fetch(`${BASE_URL}/${id}`, { method: "DELETE" });
  if (!res.ok) throw new Error("Failed to delete aircraft");
  return res.json();
}

export default function AircraftDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [aircraft, setAircraft] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [airportsData, setAirportsData] = useState([]);

  useEffect(() => {
    async function loadExtras() {
      try {
        const airportsRes = await fetch("http://localhost:8081/api/airports");

        const airportsList = await airportsRes.json();

        setAirportsData(airportsList);
      } catch (err) {
        console.error("Error fetching live airport/route data:", err);
      }
    }

    loadExtras();
  }, [aircraft]);

  // Load aircraft
  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await fetchAircraftById(id);
      setAircraft(data);
    } catch (err) {
      setError(err.message || "Failed to load aircraft");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [id]);

  async function handleDelete() {
    setConfirmOpen(false);
    try {
      await deleteAircraftById(id);
      navigate("/confirm", {
        state: {
          type: "aircraft",
          action: "deleted",
          payload: { serialNumber: aircraft?.serialNumber },
        },
      });
    } catch (err) {
      setError(err.message || "Failed to delete aircraft");
    }
  }

  // Loading / Error / Not Found
  if (loading) return <LoadingSpinner label="Loading aircraft details..." />;
  if (!aircraft) return <ErrorAlert message={error || "Aircraft not found."} />;
  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Back Button */}
      <button
        onClick={() => navigate("/aircraft")}
        className="text-xs mb-2 pt-7 pb-5 text-black"
      >
        &larr; Back to Aircraft
      </button>

      {/* Aircraft Header */}
      <div className="rounded-3xl border border-slate-800 bg-gradient-to-br from-slate-900 to-slate-950 p-6 shadow-lg">
        <div className="flex flex-col md:flex-row justify-between gap-4 mb-4">
          {/* Info Left */}
          <div>
            <div className="flex items-center gap-2 text-xs text-slate-400 mb-1">
              <span>Aircraft</span>
              <span className="px-2 py-0.5 rounded-full bg-slate-900 border border-slate-700">
                {aircraft.serialNumber}
              </span>
            </div>

            <h2 className="text-2xl pt-3 text-white font-semibold mb-1">
              {aircraft.model}
            </h2>

            <p className="text-sm text-slate-300">
              Seats: {aircraft.seatingCapacity ?? "N/A"}
            </p>
            <div className="flex items-center gap-2 text-sm text-slate-300 mb-1">
              <span>Operated By: </span>
              <span className="px-2 py-0.5 rounded-full text-sm text-slate-300 bg-slate-900 border border-slate-700">
                {aircraft.airline}
              </span>
            </div>
          </div>

          {/* Status + Buttons */}
          <div className="flex flex-col items-start md:items-end gap-2">
            <div className="text-xs text-slate-400">
              <span className="block text-[0.7rem] uppercase">Status</span>

              <span
                className={
                  aircraft.status === "Active"
                    ? "text-emerald-400 pt-10"
                    : "text-red-400 pt-10"
                }
              >
                {aircraft.status === "Active" ? "Active" : "Scheduled"}
              </span>
            </div>

            <div className="flex gap-2 pt-7 mt-5">
              <button
                onClick={() => setConfirmOpen(true)}
                className="px-3 py-1.5 text-xs rounded-full bg-red-500/80 text-slate-50 hover:bg-red-400"
              >
                Delete Aircraft
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Confirm Delete */}
      <ConfirmDialog
        open={confirmOpen}
        title={`Remove Aircraft`}
        description={
          <div className="space-y-3 text-[15px] mt-1 leading-relaxed">
            <p>
              You are about to permanently remove
              <span className="text-red-400 font-semibold ml-1">
                {aircraft.serialNumber}
              </span>
              .
            </p>

            <div className="bg-black/20 border border-white/10 rounded-lg p-3">
              <p>
                <span className="text-slate-300">Model:</span>{" "}
                <b>{aircraft.model}</b>
              </p>
              <p>
                <span className="text-slate-300">Airline:</span>{" "}
                <b>{aircraft.airline}</b>
              </p>
              <p>
                <span className="text-slate-300">Seating Capacity:</span>{" "}
                <b>{aircraft.seatingCapacity}</b>
              </p>
              <p>
                <span className="text-slate-300">Status:</span>
                <b
                  className={
                    aircraft.status === "Active"
                      ? "text-green-400"
                      : "text-yellow-400"
                  }
                >
                  {aircraft.status}
                </b>
              </p>
            </div>

            {aircraft.assignedRoutes?.length > 0 ? (
              <div className="bg-black/20 border border-white/10 rounded-lg p-3">
                <p className="font-semibold mb-1 text-slate-200">
                  Assigned Routes
                </p>
                <ul className="list-disc ml-5 space-y-1 text-slate-300 text-sm">
                  {aircraft.assignedRoutes.map((r) => (
                    <li key={r.item}>{r.item}</li>
                  ))}
                </ul>
              </div>
            ) : (
              <p className="text-slate-400 text-sm">No routes assigned.</p>
            )}

            <p className="text-red-400 text-sm mt-2">This cannot be undone.</p>
          </div>
        }
        confirmLabel="Delete Aircraft"
        cancelLabel="Cancel"
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />

      {error && <ErrorAlert message={error} />}

      {/* Assigned Routes */}
      <div className="mt-8 mb-20">
        <span className="block text-[0.7rem] uppercase tracking-wide text-black mb-3">
          Assigned Routes
        </span>

        {aircraft.assignedRoutes ? (
          <div className="rounded-2xl bg-gradient-to-b from-slate-900 to-slate-950 border border-slate-700 shadow-xl overflow-hidden">
            {aircraft.assignedRoutes.map((r, idx) => {
              const [from, to] = r.item.split("-");

              const fromAirport = airportsData.find((a) => a.code === from);
              const toAirport = airportsData.find((a) => a.code === to);

              return (
                <div
                  key={idx}
                  className="px-4 py-4 border-b border-slate-700 last:border-b-0 hover:bg-slate-800/50 transition"
                >
                  <div className="flex items-center justify-between">
                    {/* Codes */}
                    <div className="flex items-center gap-3">
                      <span className="text-slate-300 font-semibold text-sm">
                        {from}
                      </span>
                      <span className="text-slate-500 text-xs">&rarr;</span>
                      <span className="text-sky-300 font-semibold text-sm">
                        {to}
                      </span>
                    </div>

                    {/* Tag */}
                    <span className="text-[0.6rem] mt-5 uppercase tracking-wide bg-slate-900/60 border border-slate-700 px-2 py-1 rounded-full text-slate-400">
                      Route {idx + 1}
                    </span>
                  </div>

                  {/* Airport names */}
                  <div className="mt-1 text-[0.7rem] text-slate-400">
                    <span className="font-medium text-slate-300">
                      {fromAirport.name}
                    </span>
                    <span className="mx-2 text-slate-600">→</span>
                    <span className="font-medium text-sky-300">
                      {toAirport.name}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-xs text-slate-500 ml-1">No routes assigned.</p>
        )}
      </div>
    </div>
  );
}
