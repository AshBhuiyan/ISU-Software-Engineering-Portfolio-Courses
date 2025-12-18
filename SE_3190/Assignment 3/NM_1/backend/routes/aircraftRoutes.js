import express from "express";
import {
  getAllAircraft,
  getAircraftById,
  createAircraft,
  updateAircraft,
  deleteAircraft,
  getAssignedRoutesByAircraft,
} from "../controllers/aircraftController.js";

const router = express.Router();

// GET /api/aircraft - Returns all aircraft
router.get("/", getAllAircraft);

// GET /api/aircraft/assigned-routes/:model - Fetches assigned routes by aircraft model
// Must be before /:id to avoid route conflicts
router.get("/assigned-routes/:model", getAssignedRoutesByAircraft);

// GET /api/aircraft/:id - Returns one aircraft by its MongoDB _id
router.get("/:id", getAircraftById);

// POST /api/aircraft - Creates a new aircraft record
router.post("/", createAircraft);

// PUT /api/aircraft/:id - Updates an existing aircraft by _id
router.put("/:id", updateAircraft);

// DELETE /api/aircraft/:id - Removes an aircraft by _id
router.delete("/:id", deleteAircraft);

export default router;
