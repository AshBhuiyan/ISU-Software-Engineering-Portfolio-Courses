export default function ProjectDisclaimer() {
  return (
    <section
      className="
        h-screen w-full 
        bg-[#031720] 
        flex flex-col items-center justify-center 
        text-center px-6 select-none
        relative overflow-hidden
      "
    >
      {/* Background aurora glow */}
      <div
        className="
        absolute inset-0 opacity-30 pointer-events-none -z-10
        bg-[radial-gradient(circle_at_30%_20%,#22d3ee_0,transparent_50%),radial-gradient(circle_at_80%_80%,#a855f7_0,transparent_55%)]
      "
      />

      <div
        className="
          max-w-3xl mx-auto p-10 lg:p-14 rounded-2xl
          bg-black/30 backdrop-blur-lg shadow-[0_0_50px_rgba(0,0,0,0.6)]
          border border-white/10
          space-y-5 animate-fadeIn
        "
      >
        <h1 className="text-3xl md:text-4xl font-semibold text-white">
          SkyValor Airlines Project
        </h1>

        <h2 className="text-lg md:text-xl font-medium text-sky-300">
          UI and Assignment Designed by Pranava Sai Maganti
        </h2>

        <p className="text-gray-300 leading-relaxed text-[15px] md:text-base">
          All airline graphics, routes, aircraft data, visual maps, and media
          assets used within this project are intended solely for educational
          and academic evaluation.
          <span className="text-red-300 font-medium">
            {" "}
            Redistribution or public hosting is not permitted.
          </span>
        </p>

        <p className="text-gray-300 leading-relaxed text-[15px] md:text-base">
          You are allowed to reference this project in a resume or coursework
          summary, including learning outcomes and role descriptions. However,
          the{" "}
          <span className="text-rose-400 font-medium">
            live demo, codebase, and visual assets must not be published
            publicly
          </span>
          (GitHub, portfolio, YouTube, LinkedIn, etc.)
        </p>

        <hr className="border-white/10 my-4" />

        <p className="text-gray-500 text-sm">
          © {new Date().getFullYear()} SE/COM S 3190 · All Rights Reserved
        </p>
      </div>
    </section>
  );
}
