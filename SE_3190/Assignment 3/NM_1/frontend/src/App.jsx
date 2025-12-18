import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProjectDisclaimer from "./components/ProjectDisclaimer";

import Home from "./pages/Home";
import RoutesPage from "./pages/RoutesPage";
import RouteDetails from "./pages/RouteDetails";
import AircraftPage from "./pages/AircraftPage";
import AircraftDetails from "./pages/AircraftDetails";
import Confirmation from "./pages/Confirmation";
import { Toaster } from "react-hot-toast";

function App() {
  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-gray-50">
        <Navbar />

        <main className="flex-1">
          <Routes>
            <Route path="/" element={<Home />} />

            <Route path="/routes" element={<RoutesPage />} />
            <Route path="/routes/:id" element={<RouteDetails />} />

            <Route path="/aircraft" element={<AircraftPage />} />
            <Route path="/aircraft/:id" element={<AircraftDetails />} />
            <Route path="/disclaimer" element={<ProjectDisclaimer />} />

            <Route path="/confirm" element={<Confirmation />} />
          </Routes>
          <Toaster
            position="top-center"
            toastOptions={{
              style: {
                background: "#111827",
                color: "#fff",
                fontSize: "14px",
              },
            }}
          />
        </main>
      </div>
    </Router>
  );
}

export default App;
