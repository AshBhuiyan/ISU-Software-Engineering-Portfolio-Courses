import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner.jsx";
import ErrorAlert from "../components/ErrorAlert.jsx";
import ConfirmDialog from "../components/ConfirmDialog.jsx";

const BASE_URL = "http://localhost:8081/api/routes";

// -------------------------------
// Backend API Functions (LOCAL)
// -------------------------------
async function getRouteById(id) {
  const res = await fetch(`${BASE_URL}/${id}`);
  if (!res.ok) throw new Error("Failed to fetch route");
  const data = await res.json();
  console.log("Fetched route data:", data.airline);
  return data;
}

async function deleteRoute(id) {
  const res = await fetch(`${BASE_URL}/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error("Failed to delete route");
  return res.json();
}

export default function RouteDetails({ routeId }) {
  const { id } = useParams();
  const navigate = useNavigate();

  const [route, setRoute] = useState(null);
  const [flightNumber, setFlightNumber] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [mode, setMode] = useState(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await getRouteById(id);
      setRoute(data);
      setFlightNumber(data.id);
    } catch (err) {
      setError(err.message || "Failed to load route");
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
      await deleteRoute(id);
      navigate("/confirm", {
        state: {
          type: "route",
          action: "deleted",
          payload: { routeId: route?.id },
        },
      });
    } catch (err) {
      setError(err.message || "Failed to delete route");
    }
  }

  if (loading) return <LoadingSpinner label="Loading route details..." />;
  if (!route) return <ErrorAlert message={error || "Route not found."} />;
  const AIRLINES_NEED_SUFFIX = ["United", "Delta", "American", "Southwest"];

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <button
        onClick={() => navigate("/routes")}
        className="text-xs mb-2 pt-7 pb-5 text-black"
      >
        &larr; Back to Routes
      </button>

      <div className="rounded-3xl border border-slate-800 bg-gradient-to-br from-slate-900 to-slate-950 p-6 shadow-lg">
        <div className="flex flex-col md:flex-row justify-between gap-4 mb-4">
          <div>
            <div className="flex items-center gap-2 text-xs text-slate-400 mb-1">
              <span>Route</span>
            </div>
            <h2 className="text-2xl text-white font-semibold mb-1">
              {route.from.code}{" "}
              <span className="text-white text-base">&rarr;</span>{" "}
              {route.to.code}
            </h2>
            <p className="text-sm text-slate-300">
              {route.from.name} to {route.to.name}
            </p>
          </div>
          <div className="flex flex-col items-start md:items-end gap-2">
            <div className="flex gap-2">
              <button
                onClick={() => setConfirmOpen(true)}
                className="px-3 py-1.5 text-xs rounded-full bg-red-500/80 text-slate-50 hover:bg-red-400"
              >
                Delete Route
              </button>
            </div>
            {route.airline !== "SkyValor" && (
              <span className="px-2 py-0.5 rounded-full text-sm text-white bg-slate-900 border border-slate-700">
                Operated By:{" "}
                {AIRLINES_NEED_SUFFIX.includes(route.airline)
                  ? `${route.airline} Airlines`
                  : route.airline}
              </span>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 text-xs md:text-sm text-slate-300">
          <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-3">
            <div className="text-[0.7rem] uppercase text-slate-400 mb-1">
              Departure
            </div>
            <div>
              {route.from.name.replace(/International|Airport/gi, "").trim()}
            </div>
            <div className="text-slate-400 text-xs">{route.from.code}</div>
          </div>
          <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-3">
            <div className="text-[0.7rem] uppercase text-slate-400 mb-1">
              Arrival
            </div>
            <div>
              {route.to.name.replace(/International|Airport/gi, "").trim()}
            </div>
            <div className="text-slate-400 text-xs">{route.to.code}</div>
          </div>
          <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-3">
            <div className="text-[0.7rem] uppercase text-slate-400 mb-4">
              Flight Number
            </div>
            <div className="text-xs text-slate-300">
              <span className="px-2 mt-10 py-0.5 rounded-full text-white bg-slate-900 border border-slate-700">
                {route.id}
              </span>
            </div>
          </div>
          <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-3">
            <div className="text-[0.7rem] uppercase text-slate-400 mb-4">
              Aircraft Assigned
            </div>
            <div className="text-xs text-slate-300">
              <span className="px-2 mt-10 py-0.5 rounded-full text-white bg-slate-900 border border-slate-700">
                {route.aircraft}
              </span>
            </div>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title={`Delete Route ${route?.id || ""}`}
        description={
          <div className="text-sm leading-relaxed text-slate-300">
            <p className="mb-2">You are about to cancel flight:</p>

            <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-3 text-[13px] space-y-1">
              <p>
                <b>Flight:</b> <span className="text-white">{route?.id}</span>
              </p>
              <p>
                <b>Route:</b> {route?.from.code}{" "}
                <span className="text-slate-400">→</span> {route?.to.code}
              </p>
              <p>
                <b>Aircraft:</b> {route?.aircraft}
              </p>
              <p>
                <b>Passengers:</b> {route?.passengers}
              </p>
            </div>

            <p className="mt-3 text-red-300 font-medium">
              This action cannot be undone.
            </p>
          </div>
        }
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />

      {error && <ErrorAlert message={error} />}
    </div>
  );
}
