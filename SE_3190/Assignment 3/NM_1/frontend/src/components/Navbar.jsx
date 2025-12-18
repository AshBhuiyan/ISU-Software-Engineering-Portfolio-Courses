import { Link, NavLink } from "react-router-dom";
import logo from "../assets/logo.png";

export default function Navbar() {
  return (
    <header className="sticky top-0 z-50 bg-[#030712] border-b border-slate-800 shadow-[0_2px_8px_rgba(0,0,0,0.45)]">
      <div className="max-w-7xl mx-auto px-6 md:px-10 py-5 flex items-center justify-between">
        {/* Branding */}
        <Link to="/" className="flex items-center gap-4 group">
          <img
            src={logo}
            alt="SkyValor Airlines"
            className="
    h-18              /* controls navbar height */
    w-auto            /* keeps proportions */
    object-contain
    transition
    scale-200         /* visually larger without increasing navbar height */
    group-hover:scale-200
  "
          />
        </Link>

        {/* Navigation */}
        <nav className="flex items-center gap-10 ml-40 text-white text-2xl font-medium">
          {[
            { name: "Home", path: "/" },
            { name: "Routes", path: "/routes" },
            { name: "Aircraft", path: "/aircraft" },
            { name: "Disclaimer", path: "/disclaimer" },
          ].map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `
        relative px-3 py-1 rounded-full transition
        ${
          isActive
            ? "text-skyvalorAccent"
            : "text-white hover:text-black hover:bg-sky-300"
        }
      `}
            >
              {({ isActive }) => (
                <>
                  {item.name}
                  {isActive && (
                    <span className="absolute left-0 right-0 -bottom-1 mx-auto h-[3px] w-10 bg-skyvalorAccent rounded-full shadow-[0_0_10px_#22d3ee]"></span>
                  )}
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
