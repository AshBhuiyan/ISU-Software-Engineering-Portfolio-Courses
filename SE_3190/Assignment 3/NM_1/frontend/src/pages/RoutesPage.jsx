import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner.jsx";
import ErrorAlert from "../components/ErrorAlert.jsx";
import RouteCard from "../components/RouteCard.jsx";
import RouteForm from "../components/RouteForm.jsx";
import { toast } from "react-hot-toast";

const BASE_URL = "http://localhost:8081/api";

// -------------------------------
// INLINE BACKEND API FUNCTIONS
// -------------------------------

async function getRoutes() {
  const res = await fetch(`${BASE_URL}/routes`);
  if (!res.ok) throw new Error("Failed to fetch routes list");
  return res.json();
}

async function createRoute(data) {
  const res = await fetch(`${BASE_URL}/routes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  console.log(res.body);

  if (!res.ok) throw new Error("Failed to create route");
  return res.json();
}

async function updateRoute(routeNumber, data) {
  const res = await fetch(`${BASE_URL}/routes/${routeNumber}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!res.ok) throw new Error("Failed to update route");
  return res.json();
}

async function deleteRoute(id) {
  const res = await fetch(`${BASE_URL}/routes/${id}`, {
    method: "DELETE",
  });

  if (!res.ok) throw new Error("Failed to delete route");
  return res.json();
}

export default function RoutesPage() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [mode, setMode] = useState(null); // 'create' | 'edit' | 'delete'
  const [skyvalorRoutes, setSkyvalorRoutes] = useState([]);
  const [partnerRoutes, setPartnerRoutes] = useState([]);

  const navigate = useNavigate();

  async function loadRoutes() {
    setLoading(true);
    setError("");
    try {
      const data = await getRoutes();

      const prepared = data.map((r) => ({
        ...r,
        rawId: r.id,
        displayId:
          r.airline !== "SkyValor"
            ? `Operated By ${r.airline} as ${r.id}`
            : r.id,
      }));

      // Sort SkyValor first, then others, then numeric sort inside each group
      const sorted = prepared.sort((a, b) => {
        const aStartsSV = a.displayId.startsWith("SV");
        const bStartsSV = b.displayId.startsWith("SV");

        // SV goes first
        if (aStartsSV && !bStartsSV) return -1;
        if (!aStartsSV && bStartsSV) return 1;

        // Otherwise alphabetical
        return a.displayId.localeCompare(b.displayId);
      });

      const skyvalorRoutes = sorted.filter((r) => r.displayId.startsWith("SV"));
      setSkyvalorRoutes(skyvalorRoutes);
      const partnerRoutes = sorted.filter((r) =>
        r.displayId.startsWith("Operated By")
      );
      setPartnerRoutes(partnerRoutes);

      setRoutes(sorted);
    } catch (err) {
      setError(err.message || "Failed to load routes");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadRoutes();
  }, []);

  function openCreate() {
    setMode("create");
  }

  function openEdit() {
    setMode("edit");
  }

  async function handleSubmitRoute(formData) {
    try {
      let result;

      if (mode === "create") {
        result = await createRoute(formData);
      }

      if (mode === "edit") {
        result = await updateRoute(formData._id, formData);
      }

      toast.success("Route updated", { duration: 2000 });
      alert("Route Updated");

      await loadRoutes();
      setMode(null);

      setTimeout(() => {
        navigate("/confirm", {
          state: {
            type: "route",
            action: mode === "create" ? "created" : "updated",
            payload: result, // returned from backend
          },
        });
      }, 1500);
    } catch (err) {
      console.log(err);
    }
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6 mb-13">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl pt-10 font-semibold mb-1">Routes</h2>
          <p className="text-sm text-slate-400 max-w-2xl">
            Manage SkyValor routes out of your hub. Select a card to focus a
            route then use the actions to add, edit, or remove it.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={openCreate}
            className="px-3 py-1.5 text-xs rounded-full bg-skyvalorAccent text-slate-950 font-semibold hover:bg-sky-300"
          >
            Add New Route
          </button>
          <button
            onClick={openEdit}
            className="px-3 py-1.5 text-xs rounded-full bg-slate-900 border border-slate-600 text-slate-100 hover:border-skyvalorAccent"
          >
            Update Route
          </button>
        </div>
      </div>

      {error && <ErrorAlert message={error} />}

      {loading ? (
        <LoadingSpinner label="Loading routes..." />
      ) : (
        <>
          {/* SkyValor Fleet */}
          {skyvalorRoutes.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-lg font-semibold text-black">
                SkyValor Routes
              </h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {skyvalorRoutes.map((route) => (
                  <RouteCard key={route._id} route={route} />
                ))}
              </div>
            </div>
          )}

          {/* Partner Airlines */}
          {partnerRoutes.length > 0 && (
            <div className="space-y-3 mt-6">
              <h3 className="text-lg font-semibold text-black">
                Partner Airlines
              </h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {partnerRoutes.map((route) => (
                  <RouteCard key={route._id} route={route} />
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {/* Modal for create/edit */}
      {mode === "create" || mode === "edit" ? (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70">
          <div className="bg-slate-900 rounded-2xl border border-slate-700 w-full max-w-2xl p-6 shadow-xl">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg text-white font-semibold">
                {mode === "create" ? "Add New Route" : "Update Route"}
              </h3>
              <button
                onClick={() => setMode(null)}
                className="text-slate-400 hover:text-slate-200 text-xs"
              >
                Close
              </button>
            </div>
            <RouteForm
              initial={null}
              mode={mode}
              onSubmit={handleSubmitRoute}
              onCancel={() => setMode(null)}
            />
          </div>
        </div>
      ) : null}
    </div>
  );
}
