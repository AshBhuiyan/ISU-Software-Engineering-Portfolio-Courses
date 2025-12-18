import { useEffect, useState, useRef, useMemo } from "react";
import airportsData from "../data/airports.json";

const USA_ALLOWED = new Set([
  "ATL",
  "AUS",
  "BOS",
  "BWI",
  "CLT",
  "CVG",
  "DAL",
  "DEN",
  "DFW",
  "DTW",
  "EWR",
  "FLL",
  "HOU",
  "IAD",
  "IAH",
  "JFK",
  "LAS",
  "LAX",
  "LGA",
  "MCI",
  "MCO",
  "MDW",
  "MIA",
  "MSP",
  "MSY",
  "ORD",
  "PDX",
  "PGD",
  "PHL",
  "PHX",
  "RDU",
  "RSW",
  "SAN",
  "SFO",
  "SJC",
  "SLC",
  "SMF",
  "STL",
]);

export default function WorldMap({ fullScreen = false }) {
  const [routes, setRoutes] = useState([]);
  const [imgSize, setImgSize] = useState({
    w: 1,
    h: 1,
    offsetX: 0,
    offsetY: 0,
  });
  const imgRef = useRef();

  // REAL IMAGE SIZE (1740 × 1160)
  const MAP_W = 1740;
  const MAP_H = 1160;

  // --- Projection using ACTUAL displayed dimensions ---
  const project = (lat, lng) => {
    const { w, h, offsetX, offsetY } = imgSize;

    // Real map usable drawing area inside image
    const X_PAD_LEFT = 0.07; // 7% left empty
    const X_PAD_RIGHT = 0.1; // 10% right empty
    const Y_PAD_TOP = 0.25; // 25% top gap
    const Y_PAD_BOTTOM = 0.28; // 28% bottom gap

    const drawW = w * (1 - X_PAD_LEFT - X_PAD_RIGHT);
    const drawH = h * (1 - Y_PAD_TOP - Y_PAD_BOTTOM);

    // Geographic projection bounds
    const MIN_LNG = -170;
    const MAX_LNG = 178;
    const MIN_LAT = -55;
    const MAX_LAT = 77;

    const x =
      ((lng - MIN_LNG) / (MAX_LNG - MIN_LNG)) * drawW +
      offsetX +
      w * X_PAD_LEFT;
    const y =
      ((MAX_LAT - lat) / (MAX_LAT - MIN_LAT)) * drawH + offsetY + h * Y_PAD_TOP;

    return { x, y };
  };

  // Build airport lookup
  const airportLookup = {};
  airportsData.forEach((a) => {
    if (a.iata_code) {
      airportLookup[a.iata_code.toUpperCase()] = {
        lat: a.lat,
        lng: a.lng,
        name: a.name,
      };
    }
  });

  useEffect(() => {
    const fetchRoutes = async () => {
      try {
        const res = await fetch("http://localhost:8081/api/routes");
        const data = await res.json();
        setRoutes(data);
        console.log(data[0]);
      } catch (err) {
        console.error("Error fetching routes from backend:", err);
      }
    };

    fetchRoutes();
  }, []);

  const curveOffset = (from, to) => {
    const d = Math.hypot(to.x - from.x, to.y - from.y);
    return d * 0.1; // lower → more realistic long haul arcs
  };

  // Compute how the world map image is “contained”
  const handleImageLoad = () => {
    const containerW = imgRef.current.clientWidth;
    const containerH = imgRef.current.clientHeight;

    // scale image proportionally
    const scale = Math.min(containerW / MAP_W, containerH / MAP_H);
    const displayedW = MAP_W * scale;
    const displayedH = MAP_H * scale;

    const offsetX = (containerW - displayedW) / 2;
    const offsetY = (containerH - displayedH) / 2;

    setImgSize({
      w: displayedW,
      h: displayedH,
      offsetX,
      offsetY,
    });
  };

  const displayRoutes = useMemo(() => {
    const unique = new Map();

    const SHOW_DOMESTIC = new Set([
      "ATL",
      "AUS",
      "BOS",
      "BWI",
      "CLT",
      "CVG",
      "DAL",
      "DEN",
      "DFW",
      "DTW",
      "EWR",
      "FLL",
      "HOU",
      "IAD",
      "IAH",
      "JFK",
      "LAS",
      "LAX",
      "LGA",
      "MCI",
      "MCO",
      "MDW",
      "MIA",
      "MSP",
      "MSY",
      "ORD",
      "PDX",
      "PGD",
      "PHL",
      "PHX",
      "RDU",
      "RSW",
      "SAN",
      "SFO",
      "SJC",
      "SLC",
      "SMF",
      "STL",
    ]);

    for (const r of routes) {
      const domestic =
        USA_ALLOWED.has(r.from.code) && USA_ALLOWED.has(r.to.code);

      // Only skip if domestic AND not in allowed list
      if (
        domestic &&
        !(SHOW_DOMESTIC.has(r.from.code) || SHOW_DOMESTIC.has(r.to.code))
      )
        continue;

      const key = [r.from.code, r.to.code].sort().join("-");

      if (!unique.has(key)) unique.set(key, r);
    }

    return [...unique.values()];
  }, [routes]);

  return (
    <div
      className={
        fullScreen
          ? "flex items-center justify-center w-full h-full"
          : "relative w-full aspect-[4/3] rounded-2xl overflow-hidden"
      }
    >
      <div className="relative w-[90vw] max-w-[1500px] aspect-[4/3] rounded-2xl overflow-hidden">
        <img
          ref={imgRef}
          src="/world-map.png"
          alt="World Map"
          className="absolute inset-0 w-full h-full object-contain opacity-60"
          onLoad={handleImageLoad}
        />

        <svg
          className="absolute inset-0 w-full h-full pointer-events-none"
          viewBox={`0 0 ${imgSize.w} ${imgSize.h}`}
          preserveAspectRatio="none"
        >
          {displayRoutes.map((route, idx) => {
            const from = project(route.from.lat, route.from.lng);
            const to = project(route.to.lat, route.to.lng);
            const offset = curveOffset(from, to);

            const midX = (from.x + to.x) / 2;
            const midY = (from.y + to.y) / 2 - offset;

            return (
              <g key={idx}>
                <path
                  d={`M ${from.x} ${from.y} Q ${midX} ${midY}, ${to.x} ${to.y}`}
                  stroke={
                    route.airline === "SkyValor"
                      ? "#08E8FF" // Neon Cyan
                      : route.airline === "United"
                      ? "#4169E1" // Royal electric blue
                      : route.airline === "Delta"
                      ? "#FF1744" // Bright red
                      : route.airline === "American"
                      ? "#3EC7FF" // Bright sky blue
                      : route.airline === "Southwest"
                      ? "#FFD700" // Pure gold
                      : route.airline === "Air India"
                      ? "#FF4C4C" // Fire red
                      : route.airline === "Emirates"
                      ? "#00C38A" // Emerald turquoise
                      : route.airline === "Qatar Airways"
                      ? "#E10086" // Bright raspberry wine
                      : route.airline === "British Airways"
                      ? "#5B8CFF" // Flash light blue
                      : route.airline === "Etihad"
                      ? "#FFBD59" // Bright amber-gold
                      : "#FFFFFF" // fallback bright white
                  }
                  strokeWidth="2.8"
                  opacity="0.5"
                  fill="none"
                />
                <circle cx={from.x} cy={from.y} r="4" fill="#22d3ee" />
                <circle cx={to.x} cy={to.y} r="4" fill="#22d3ee" />
              </g>
            );
          })}
        </svg>
      </div>
    </div>
  );
}
