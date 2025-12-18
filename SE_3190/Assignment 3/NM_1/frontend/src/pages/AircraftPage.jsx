import { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner.jsx";
import ErrorAlert from "../components/ErrorAlert.jsx";
import AircraftCard from "../components/AircraftCard.jsx";
import AircraftForm from "../components/AircraftForm.jsx";
import skyvalor from "../assets/logo.png";
import ai from "../assets/ai.png";

const BASE_URL = "http://localhost:8081/api";

// -------------------------------
// INLINE BACKEND API FUNCTIONS
// -------------------------------

async function getAircraftList() {
  const res = await fetch(`${BASE_URL}/aircraft`);
  if (!res.ok) throw new Error("Failed to fetch aircraft list");
  return res.json();
}

async function createAircraft(data) {
  const res = await fetch(`${BASE_URL}/aircraft`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!res.ok) throw new Error("Failed to create aircraft");
  return res.json();
}

async function updateAircraft(id, data) {
  const res = await fetch(`${BASE_URL}/aircraft/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!res.ok) throw new Error("Failed to update aircraft");
  return res.json();
}

const AirLineLogos = {
  American:
    "https://wallpapers.com/images/hd/american-airlines-2013-logo-z6s9ch564m54idy0.jpg",
  United:
    "https://logos-world.net/wp-content/uploads/2020/11/United-Airlines-Logo.png",
  Southwest:
    "https://bodybilt.com/wp-content/uploads/2022/03/Southwest-Airlines-Logo-300x169.png",
  Delta:
    "https://1000logos.net/wp-content/uploads/2017/09/Delta-Air-Lines-Logo.png",
  SkyValor: skyvalor,
  Emirates:
    "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d0/Emirates_logo.svg/1200px-Emirates_logo.svg.png",
  Etihad:
    "https://liveandletsfly.boardingarea.com/wp-content/uploads/2018/06/Etihad-Logo.png",
  British:
    "https://www.ibizainsider.co.uk/wp-content/uploads/2023/12/British_Airways-Logo.wine_.png",
  AI: ai,
  Qatar:
    "https://images.seeklogo.com/logo-png/11/2/qatar-airways-logo-png_seeklogo-114154.png",
};

export default function AircraftPage() {
  const [aircraft, setAircraft] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(null);
  const [mode, setMode] = useState(null);

  const navigate = useNavigate();

  async function loadAircraft() {
    setLoading(true);
    setError("");
    try {
      const data = await getAircraftList();
      setAircraft(data || []);
    } catch (err) {
      setError(err.message || "Failed to load aircraft");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAircraft();
  }, []);

  const grouped = useMemo(() => {
    const groups = {};
    for (const a of aircraft) {
      const key = a.airline || "Unknown Airline";
      if (!groups[key]) groups[key] = [];
      groups[key].push(a);
    }
    return groups;
  }, [aircraft]);

  function handleCardSelect(ac) {
    setSelected(ac);
  }

  function openCreate() {
    setSelected(null);
    setMode("create");
  }

  function openEdit() {
    setMode("edit");
  }

  async function handleSubmitAircraft(formData) {
    try {
      setError("");
      let result;
      if (mode === "create") {
        result = await createAircraft(formData);
        navigate("/confirm", {
          state: { type: "aircraft", action: "created", payload: result },
        });
      } else if (mode === "edit" && selected) {
        result = await updateAircraft(selected._id, formData);
        navigate("/confirm", {
          state: { type: "aircraft", action: "updated", payload: result },
        });
      }
      setMode(null);
      setSelected(null);
      await loadAircraft();
    } catch (err) {
      setError(err.message || "Failed to save aircraft");
    }
  }

  function normalizeAirlineName(name) {
    if (!name) return "Unknown";

    if (name == "Air India") return "AI";

    return name
      .replace(/airways?/i, "")
      .replace(/airlines?/i, "")
      .trim();
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6 mb-10">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="pt-10 text-2xl font-semibold mb-1">Aircraft</h2>
          <p className="text-sm text-slate-400 max-w-2xl">
            Manage SkyValor fleet by tail number. Grouped by model so you can
            see capacity and assignment coverage quickly.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={openCreate}
            className="px-3 py-1.5 text-xs rounded-full bg-skyvalorAccent text-slate-950 font-semibold hover:bg-sky-300"
          >
            Add New Aircraft
          </button>
          <button
            onClick={openEdit}
            className="px-3 py-1.5 text-xs rounded-full bg-slate-900 border border-slate-600 text-slate-100 hover:border-skyvalorAccent"
          >
            Update Aircraft
          </button>
        </div>
      </div>

      {error && <ErrorAlert message={error} />}

      {loading ? (
        <LoadingSpinner label="Loading aircraft..." />
      ) : Object.keys(grouped).length === 0 ? (
        <div className="text-sm text-slate-400">
          No aircraft found in database.
        </div>
      ) : (
        <div className="space-y-10">
          {Object.entries(grouped).map(([airline, list]) => (
            <div key={airline} className="space-y-3">
              <img
                src={
                  AirLineLogos[normalizeAirlineName(airline)] ||
                  AirLineLogos["SkyValor"]
                }
                alt={airline}
                className={
                  normalizeAirlineName(airline) === "British" ||
                  normalizeAirlineName(airline) === "AI"
                    ? "h-56 object-contain mx-auto drop-shadow -mt-15" // class for British or Air India
                    : normalizeAirlineName(airline) === "Etihad"
                    ? "h-35 object-contain mx-auto drop-shadow" // class for Etihad
                    : "h-35 object-contain mx-auto drop-shadow" // class for all others
                }
              />
              <div
                className={
                  normalizeAirlineName(airline) === "Etihad" ||
                  normalizeAirlineName(airline) === "Emirates"
                    ? "grid grid-cols-2 gap-4 mt-6" // class for Etihad or Emirates
                    : "grid grid-cols-2 gap-4 -mt-6" // class for all others
                }
              >
                {list.map((a) => (
                  <AircraftCard
                    key={a._id}
                    aircraft={a}
                    selected={selected?._id === a._id}
                    onSelect={handleCardSelect}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal for create/edit */}
      {mode === "create" || mode === "edit" ? (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70">
          <div className="bg-slate-900 rounded-2xl border border-slate-700 w-full max-w-2xl p-6 shadow-xl">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg text-white font-semibold">
                {mode === "create" ? "Add New Aircraft" : "Update Aircraft"}
              </h3>
              <button
                onClick={() => setMode(null)}
                className="text-slate-400 hover:text-slate-200 text-xs"
              >
                Close
              </button>
            </div>
            <AircraftForm
              initial={null}
              onSubmit={handleSubmitAircraft}
              onCancel={() => setMode(null)}
              mode={mode}
            />
          </div>
        </div>
      ) : null}
    </div>
  );
}
