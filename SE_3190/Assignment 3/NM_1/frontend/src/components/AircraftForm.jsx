import { useState, useEffect } from "react";

const BASE_URL = "http://localhost:8081/api";

const AIRLINE_PREFIX = {
  UA: "United",
  AA: "American",
  DL: "Delta",
  WN: "Southwest",
  BA: "British Airways",
  EK: "Emirates",
  EY: "Etihad",
  AI: "Air India",
  QR: "Qatar Airways",
  SV: "SkyValor",
};

const emptyAircraft = {
  serialNumber: "",
  brand: "",
  model: "",
  airlinePrefix: "",
  airline: "",
  seatingCapacity: "",
  range: "",
  status: "",
  assignedRoutes: [],
};

export default function AircraftForm({ initial, onSubmit, onCancel, mode }) {
  const isEdit = mode === "edit";

  const [form, setForm] = useState(initial || emptyAircraft);

  const [airportsData, setAirportsData] = useState([]);
  const [aircraftsInMarket, setAircraftsInMarket] = useState([]);
  const [modelsList, setModelList] = useState([]);

  const [routeFrom, setRouteFrom] = useState("");
  const [routeTo, setRouteTo] = useState("");

  const [searchInput, setSearchInput] = useState("");
  const [searchStatus, setSearchStatus] = useState(null);

  // -----------------------------------
  // Load airports & market aircraft data
  // -----------------------------------
  useEffect(() => {
    async function loadData() {
      try {
        const res1 = await fetch(`${BASE_URL}/airports`);
        const airportJson = await res1.json();
        setAirportsData(Array.isArray(airportJson) ? airportJson : []);

        const res2 = await fetch(`${BASE_URL}/aircraftsInMarket`);
        if (!res2.ok) throw new Error("Failed to load aircraft market");
        const marketJson = await res2.json();
        setAircraftsInMarket(Array.isArray(marketJson) ? marketJson : []);
      } catch (err) {
        console.error(err);
        setAircraftsInMarket([]); // prevents crash
      }
    }

    loadData();
  }, []);

  // Populate models when brand changes
  useEffect(() => {
    const currentBrand = aircraftsInMarket.find((b) => b.brand === form.brand);
    setModelList(currentBrand ? currentBrand.aircrafts : []);
  }, [form.brand, aircraftsInMarket]);

  // -----------------------------------
  // Autofill search (works like Route form)
  // -----------------------------------
  async function searchAircraft() {
    if (!searchInput.trim()) return;

    const res = await fetch(
      `${BASE_URL}/aircraft/search/${searchInput.trim()}`
    );
    const data = await res.json();

    if (!data || !data.serialNumber) {
      setSearchStatus("not_found");
      return;
    }

    // Autofill
    const prefixEntry = Object.entries(AIRLINE_PREFIX).find(
      ([_, n]) => n === data.airline
    );

    setForm({
      _id: data._id,
      serialNumber: data.serialNumber,
      brand: data.brand || "",
      model: data.model,
      airlinePrefix: prefixEntry ? prefixEntry[0] : "",
      airline: data.airline,
      seatingCapacity: data.seatingCapacity,
      range: data.range,
      status: data.status,
      assignedRoutes: data.assignedRoutes || [],
    });

    setSearchStatus("found");
  }

  // -----------------------------------
  // Route Add/Remove logic
  // -----------------------------------
  function handleAddRoute() {
    if (!routeFrom || !routeTo) return;
    const item = `${routeFrom}-${routeTo}`;

    if (form.assignedRoutes.some((r) => r.item === item)) return;

    setForm((prev) => ({
      ...prev,
      assignedRoutes: [...prev.assignedRoutes, { item }],
    }));

    setRouteFrom("");
    setRouteTo("");
  }

  function handleRemoveRoute(rm) {
    setForm((prev) => ({
      ...prev,
      assignedRoutes: prev.assignedRoutes.filter((r) => r.item !== rm),
    }));
  }

  // -----------------------------------
  // Save
  // -----------------------------------
  async function createAircraft(payload) {
    const res = await fetch(`${BASE_URL}/aircraft`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      alert("Aircraft Added");
      onCancel();
    } else {
      alert("Add failed");
    }
    onCancel();
  }

  async function updateAircraft(id, payload) {
    const res = await fetch(`${BASE_URL}/aircraft/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      alert("Aircraft Updated");
      onCancel();
    } else {
      alert("Update Failed");
    }
  }

  function handleSubmit(e) {
    e.preventDefault();

    const payload = {
      serialNumber: form.serialNumber.trim(),
      model: form.model,
      airline: AIRLINE_PREFIX[form.airlinePrefix] || form.airline,
      seatingCapacity: Number(form.seatingCapacity),
      status: form.status,
      assignedRoutes: form.assignedRoutes,
    };

    if (mode === "edit" && form._id) {
      updateAircraft(form._id, payload); // PUT request
    } else {
      createAircraft(payload); // POST request
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="w-[90%] max-w-[1400px] bg-[#0b162a] p-10 rounded-xl shadow-xl text-white"
    >
      {isEdit && (
        <div className="mb-6">
          <label className="text-xs">Search Aircraft to Edit</label>

          <div className="flex gap-2 mt-1">
            <input
              className="flex-1 rounded-lg bg-slate-900 border border-slate-700 px-3 py-2"
              placeholder="Enter Serial Number"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
            <button
              type="button"
              onClick={searchAircraft}
              className="px-4 py-2 bg-skyvalorAccent rounded-lg font-semibold text-white hover:text-black hover:bg-sky-300"
            >
              Search
            </button>
          </div>

          {searchStatus === "not_found" && (
            <p className="text-red-400 text-xs mt-1">No aircraft found.</p>
          )}
          {searchStatus === "found" && (
            <p className="text-green-300 text-xs mt-1">Aircraft loaded.</p>
          )}
        </div>
      )}

      {/* Grid form */}
      <div className="grid grid-cols-2 gap-6">
        <div>
          <label className="text-xs">Serial Number</label>
          <input
            name="serialNumber"
            className="w-full bg-slate-900 border px-3 py-2 rounded"
            value={form.serialNumber}
            onChange={(e) => setForm({ ...form, serialNumber: e.target.value })}
          />
        </div>

        <div>
          <label className="text-xs">Brand</label>
          <select
            className="w-full h-10 bg-slate-900 border px-3 py-2 rounded"
            value={form.brand}
            onChange={(e) => setForm({ ...form, brand: e.target.value })}
          >
            <option value="">Select brand</option>
            {aircraftsInMarket.map((x) => (
              <option key={x.brand}>{x.brand}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="text-xs">Model</label>
          <select
            className="w-full h-10 bg-slate-900 border px-3 py-2 rounded"
            value={form.model}
            onChange={(e) => setForm({ ...form, model: e.target.value })}
          >
            <option value="">Select model</option>
            {modelsList.map((m) => (
              <option key={m}>{m}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="text-xs">Airline</label>
          <select
            className="w-full h-10 bg-slate-900 border px-3 py-2 rounded"
            value={form.airlinePrefix}
            onChange={(e) =>
              setForm({
                ...form,
                airlinePrefix: e.target.value,
                airline: AIRLINE_PREFIX[e.target.value],
              })
            }
          >
            <option value="">Select airline</option>
            {Object.keys(AIRLINE_PREFIX).map((code) => (
              <option key={code}>{code}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="text-xs">Seating Capacity</label>
          <input
            type="number"
            min="0"
            className="w-full bg-slate-900 border px-3 py-2 rounded"
            value={form.seatingCapacity}
            onChange={(e) =>
              setForm({ ...form, seatingCapacity: e.target.value })
            }
          />
        </div>

        <div>
          <label className="text-xs">Status</label>
          <select
            className="w-full h-10 bg-slate-900 border px-3 py-2 rounded"
            value={form.status}
            onChange={(e) => setForm({ ...form, status: e.target.value })}
          >
            <option value="">Select status</option>
            <option>Active</option>
            <option>Scheduled</option>
            <option>In Maintenance</option>
          </select>
        </div>
      </div>

      {/* Assign Routes */}
      <div className="mt-5">
        <label className="text-xs">Assign Routes</label>

        <div className="flex gap-3">
          <select
            value={routeFrom}
            className="flex-1 bg-slate-900 border px-3 py-2 rounded"
            onChange={(e) => setRouteFrom(e.target.value)}
          >
            <option value="">From</option>
            {airportsData.map((a) => (
              <option key={a.code}>{a.code}</option>
            ))}
          </select>

          <select
            value={routeTo}
            className="flex-1 bg-slate-900 border px-3 py-2 rounded"
            onChange={(e) => setRouteTo(e.target.value)}
          >
            <option value="">To</option>
            {airportsData.map((a) => (
              <option key={a.code}>{a.code}</option>
            ))}
          </select>

          <button
            type="button"
            onClick={handleAddRoute}
            className="px-4 py-2 bg-skyvalorAccent rounded font-semibold text-white hover:text-black hover:bg-sky-300"
          >
            Add
          </button>
        </div>

        {form.assignedRoutes.length > 0 ? (
          form.assignedRoutes.map((r) => {
            const [from, to] = r.item.split("-");
            return (
              <div
                key={r.item}
                className="flex justify-between px-3 py-1 mt-1 bg-slate-900 border rounded"
              >
                <span>
                  {from} → {to}
                </span>
                <button
                  className="text-red-400 text-xs"
                  onClick={() => handleRemoveRoute(r.item)}
                >
                  remove
                </button>
              </div>
            );
          })
        ) : (
          <p className="text-[11px] text-slate-500 mt-1">
            No routes assigned yet.
          </p>
        )}
      </div>

      {/* Submit */}
      <div className="flex justify-end gap-3 pt-6">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 bg-slate-800 rounded"
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-5 py-2 bg-skyvalorAccent font-bold rounded hover:bg-sky-300 hover:text-black"
        >
          Save Aircraft
        </button>
      </div>
    </form>
  );
}
