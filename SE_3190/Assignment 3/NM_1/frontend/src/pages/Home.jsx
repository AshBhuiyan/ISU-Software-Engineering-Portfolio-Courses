import WorldMap from "../pages/Map";

export default function Home() {
  return (
    <div className="fixed inset-0 overflow-hidden">
      <div
        className="
    relative h-screen w-full overflow-hidden
    bg-[#031720]
  "
      >
        {/* SAME gradient as Map, identical stops and opacity */}
        <div
          className="
      absolute inset-0
      bg-[radial-gradient(circle_at_20%_20%,#22d3ee_0,transparent_55%),radial-gradient(circle_at_80%_80%,#a855f7_0,transparent_55%)]
      opacity-30
      pointer-events-none
      -z-10
    "
        />

        {/* MAP */}
        <div
          className="fixed top-[8%] left-1/2 -translate-x-1/2 
               w-full h-full z-10 rounded-3xl overflow-hidden"
        >
          <WorldMap fullScreen />
        </div>

        {/* TEXT */}
        <div
          className="absolute z-20 top-[16%] left-[18%]
                space-y-5 text-white drop-shadow-[0_4px_16px_rgba(0,0,0,0.6)]"
        >
          <h1 className="text-5xl md:text-7xl font-semibold leading-snug">
            SkyValor Operations Console
          </h1>
          <p className="text-slate-200 text-base md:text-2xl max-w-xl">
            Visualize active routes coming directly from your backend. Interact
            with the network map and explore live operational data.
          </p>
        </div>
      </div>
    </div>
  );
}
