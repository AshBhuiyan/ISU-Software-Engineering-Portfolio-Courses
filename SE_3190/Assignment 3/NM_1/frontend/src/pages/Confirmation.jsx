import { useLocation, useNavigate } from "react-router-dom";

export default function ConfirmationPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state || {};

  const { type, action, payload } = state;

  function label() {
    if (!type || !action) return "Operation completed.";
    const subject =
      type === "route"
        ? `Route ${payload?.routeId || payload?.route?.routeId || ""}`
        : `Aircraft ${
            payload?.serialNumber || payload?.aircraft?.serialNumber || ""
          }`;

    switch (action) {
      case "created":
        return `${subject} has been created successfully.`;
      case "updated":
        return `${subject} has been updated successfully.`;
      case "deleted":
        return `${subject} has been deleted successfully.`;
      default:
        return "Operation completed successfully.";
    }
  }

  return (
    <div className="max-w-md mx-auto">
      <div className="mt-10 rounded-3xl border border-emerald-500/60 bg-emerald-500/10 px-6 py-8 text-center shadow-lg shadow-emerald-500/20">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500/20 border border-emerald-400/70">
          <span className="text-2xl">✓</span>
        </div>
        <h2 className="text-xl font-semibold mb-2">Success</h2>
        <p className="text-sm text-black mb-4">{label()}</p>

        {type && (
          <div className="text-xs text-black mb-4">
            <div className="font-semibold mb-1">Details</div>
            <pre className="text-[0.7rem] border border-slate-800 rounded-xl px-3 py-2 text-left overflow-x-auto">
              {JSON.stringify(payload, null, 2)}
            </pre>
          </div>
        )}

        <div className="flex flex-wrap justify-center gap-3 mt-4">
          <button
            onClick={() => navigate("/")}
            className="px-4 py-1.5 text-xs rounded-full bg-skyvalorAccent text-slate-950 font-semibold hover:bg-sky-300"
          >
            Back to Home
          </button>
          <button
            onClick={() =>
              navigate(type === "aircraft" ? "/aircraft" : "/routes")
            }
            className="px-4 py-1.5 text-xs rounded-full bg-slate-900 border border-slate-600 text-slate-100 hover:border-skyvalorAccent"
          >
            Back to {type === "aircraft" ? "Aircraft" : "Routes"}
          </button>
        </div>
      </div>
    </div>
  );
}
