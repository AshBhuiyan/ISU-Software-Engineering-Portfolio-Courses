import { useState, useEffect } from "react";

const BASE_URL = "http://localhost:8081/api";

const AIRLINE_MAP = {
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

export default function RouteForm({ initial, mode, onSubmit, onCancel }) {
  const [airports, setAirports] = useState([]);
  const [aircrafts, setAircrafts] = useState([]);
  const [filteredAircraft, setFilteredAircraft] = useState([]);
  const [routeFound, setRouteFound] = useState(null);
  const [searchInput, setSearchInput] = useState("");
  const [searchStatus, setSearchStatus] = useState(null);

  async function fetchRouteByFlightId() {
    if (!searchInput) return;

    const res = await fetch(`${BASE_URL}/routes/search/${searchInput}`);
    const data = await res.json();

    if (!data?._id) {
      setSearchStatus("not_found");
      return;
    }

    setRouteFound(data._id);

    // Autofill form
    setForm({
      airlinePrefix: data.id.slice(0, 2),
      routeNumber: data.id.slice(2),
      aircraft: data.aircraft,
      from: data.from.code,
      to: data.to.code,
      passengers: data.passengers,
      aircraftSerialNumber: data.aircraftSerialNumber,
    });

    setFromData(data.from);
    setToData(data.to);
    setDistance(data.distance_miles);
    setAircraft(data.aircraft);
    setSearchStatus("found");
  }

  // -------------------------------
  // Form initialization (CREATE + EDIT)
  // -------------------------------
  const [form, setForm] = useState({
    airlinePrefix: initial ? initial.id.slice(0, 2) : "",
    routeNumber: initial ? initial.id.slice(2) : "",
    from: initial?.from?.code || "",
    to: initial?.to?.code || "",
    passengers: initial?.passengers || "",
    aircraft: initial?.aircraft,
  });

  const [fromData, setFromData] = useState(initial?.from || null);
  const [toData, setToData] = useState(initial?.to || null);
  const [distance, setDistance] = useState(initial?.distance_miles || 0);
  const [aircraft, setAircraft] = useState(initial?.from || null);

  const updateField = (key, value) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  // -------------------------------
  // Fetch Airports + Aircrafts
  // -------------------------------
  const [airlineFromPrefix, setAirlineFromPrefix] = useState("");

  useEffect(() => {
    if (form.airlinePrefix) {
      setAirlineFromPrefix(
        AIRLINE_MAP[form.airlinePrefix] || "Unknown Airline"
      );
    } else {
      setAirlineFromPrefix("");
    }
  }, [form.airlinePrefix]);

  useEffect(() => {
    fetch(`${BASE_URL}/airports`)
      .then((res) => res.json())
      .then(setAirports);

    fetch(`${BASE_URL}/aircraft`)
      .then((res) => res.json())
      .then(setAircrafts);
  }, []);

  // Filter aircraft based on prefix airline
  useEffect(() => {
    if (form.airlinePrefix) {
      setFilteredAircraft(
        aircrafts.filter((a) => a.airline === AIRLINE_MAP[form.airlinePrefix])
      );
    }
  }, [form.airlinePrefix, aircrafts]);

  // Autofill airport details
  useEffect(() => {
    setFromData(airports.find((a) => a.code === form.from) || null);
  }, [form.from, airports]);
  useEffect(() => {
    setToData(airports.find((a) => a.code === form.to) || null);
  }, [form.to, airports]);

  // -------------------------------
  // Auto-calculate Distance
  // -------------------------------
  function haversine(a, b) {
    const R = 3958.8;
    const toRad = (x) => (x * Math.PI) / 180;
    const dLat = toRad(b.lat - a.lat);
    const dLng = toRad(b.lng - a.lng);
    return (
      2 *
      R *
      Math.asin(
        Math.sqrt(
          Math.sin(dLat / 2) ** 2 +
            Math.cos(toRad(a.lat)) *
              Math.cos(toRad(b.lat)) *
              Math.sin(dLng / 2) ** 2
        )
      )
    ).toFixed(2);
  }
  useEffect(() => {
    if (fromData && toData) setDistance(haversine(fromData, toData));
  }, [fromData, toData]);

  // -------------------------------
  // SUBMIT (CREATE / UPDATE)
  // -------------------------------
  async function handleSubmit(e) {
    e.preventDefault();

    const fullId = form.airlinePrefix + form.routeNumber;

    const selectedAircraft = aircrafts.find((a) => a.model === form.aircraft);

    if (!selectedAircraft) {
      alert("Please select a valid aircraft.");
      return;
    }

    console.log("Prefix:", form.airlinePrefix);
    console.log("Prefix airline:", AIRLINE_MAP[form.airlinePrefix]);
    console.log("Selected aircraft:", selectedAircraft);
    console.log("Will save airline:", selectedAircraft?.airline);

    const payload = {
      id: fullId,
      airline: airlineFromPrefix,
      aircraft: selectedAircraft.model,
      passengers: Number(form.passengers),
      distance_miles: Number(distance),
      from: fromData,
      to: toData,
    };

    // Include _id if editing and route was found via search
    if (mode === "edit" && routeFound) {
      payload._id = routeFound;
    }

    // Only close if backend succeeded
    onSubmit(payload);
    onCancel();
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3 text-white">
      {/* Flight Number */}
      {mode === "edit" && !initial && (
        <div className="mb-3">
          <label className="block text-xs mb-1">Find Flight to Update</label>

          <div className="flex gap-2">
            <input
              className="flex-1 rounded-lg bg-slate-900 border border-slate-700 px-3 py-2 text-sm"
              placeholder="SV683"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value.toUpperCase())}
            />

            <button
              type="button"
              onClick={fetchRouteByFlightId}
              className="px-4 py-2 rounded-lg bg-skyvalorAccent text-white font-semibold hover:bg-sky-300"
            >
              Search
            </button>
          </div>

          <div className="flex flex-col">
            <label className="text-xs text-gray-300 mb-1">
              Airline (Prefix)
            </label>
            <input
              type="text"
              value={airlineFromPrefix}
              readOnly
              className="bg-gray-700 text-gray-200 px-3 py-2 rounded text-sm border border-gray-600"
            />
          </div>

          {searchStatus === "searching" && (
            <p className="text-yellow-300 text-xs mt-1">Searching route...</p>
          )}
          {searchStatus === "not_found" && (
            <p className="text-red-400 text-xs mt-1">
              No route found with that number.
            </p>
          )}
          {searchStatus === "found" && (
            <p className="text-green-400 text-xs mt-1">
              Route matched. You may now change flight details below.
              <br />
              <span className="text-amber-500">
                Route _id:{" "}
                <strong>
                  <em>{routeFound}</em>
                </strong>
              </span>
            </p>
          )}
        </div>
      )}

      <div>
        <label className="block text-xs mb-1">Flight Number</label>
        <div className="flex items-center gap-2">
          <select
            className="w-[80px] rounded-lg bg-slate-900 border border-slate-700 px-2 py-2 text-sm"
            value={form.airlinePrefix}
            onChange={(e) => updateField("airlinePrefix", e.target.value)}
            disabled={mode === "edit"}
            required
          >
            <option value="">Airline</option>
            {Object.keys(AIRLINE_MAP).map((code) => (
              <option key={code} value={code}>
                {code}
              </option>
            ))}
          </select>

          <input
            className="w-[70px] rounded-lg bg-slate-900 border border-slate-700 px-2 py-2 text-sm"
            placeholder="683"
            value={form.routeNumber}
            disabled={false}
            onChange={(e) =>
              updateField("routeNumber", e.target.value.replace(/\D/g, ""))
            }
            required
          />
        </div>

        {form.airlinePrefix && form.routeNumber && (
          <p className="text-[11px] text-sky-300 mt-1">
            Generated Route ID: <b>{form.airlinePrefix + form.routeNumber}</b>
          </p>
        )}
      </div>

      {/* From + To Airport */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs mb-1">Departure Airport</label>
          <select
            className="w-full rounded-lg bg-slate-900 border border-slate-700 px-3 py-2 text-sm"
            value={form.from}
            onChange={(e) => updateField("from", e.target.value)}
            required
          >
            <option value="">Select</option>
            {airports.map((a) => (
              <option key={a.code} value={a.code}>
                {a.code}
              </option>
            ))}
          </select>

          {fromData && (
            <div className="text-[11px] text-slate-300 mt-1">
              <p>{fromData.name}</p>
              <p>
                Lat: {fromData.lat} | Lng: {fromData.lng}
              </p>
            </div>
          )}
        </div>

        <div>
          <label className="block text-xs mb-1">Arrival Airport</label>
          <select
            className="w-full rounded-lg bg-slate-900 border border-slate-700 px-3 py-2 text-sm"
            value={form.to}
            onChange={(e) => updateField("to", e.target.value)}
            required
          >
            <option value="">Select</option>
            {airports.map((a) => (
              <option key={a.code} value={a.code}>
                {a.code}
              </option>
            ))}
          </select>

          {toData && (
            <div className="text-[11px] text-slate-300 mt-1">
              <p>{toData.name}</p>
              <p>
                Lat: {toData.lat} | Lng: {toData.lng}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Distance */}
      {distance > 0 && (
        <p className="text-xs text-green-400">
          Distance: <b>{distance} miles</b>
        </p>
      )}

      {/* Aircraft */}
      <div>
        <label className="block text-xs mb-1">Select Aircraft</label>
        <select
          className="w-full rounded-lg bg-slate-900 border border-slate-700 px-3 py-2 text-sm"
          value={form.aircraft}
          onChange={(e) => updateField("aircraft", e.target.value)}
          required
        >
          <option value="">Select</option>
          {filteredAircraft.map((a) => (
            <option key={a.model} value={a.model}>
              {a.model} ({a.seatingCapacity} seats)
            </option>
          ))}
        </select>
      </div>

      {/* Passengers */}
      <div>
        <label className="block text-xs mb-1">Passengers</label>
        <input
          type="number"
          className="w-full rounded-lg bg-slate-900 border border-slate-700 px-3 py-2 text-sm"
          value={form.passengers}
          onChange={(e) => updateField("passengers", e.target.value)}
          required
        />
      </div>

      {/* Buttons */}
      <div className="flex justify-end gap-3 pt-3">
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 text-xs rounded-full bg-slate-800 hover:bg-slate-700"
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-1.5 text-xs rounded-full bg-skyvalorAccent text-white font-semibold hover:bg-sky-300"
        >
          Save Route
        </button>
      </div>
    </form>
  );
}
