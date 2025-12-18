import express from "express";
import {
  getAllRoutes,
  getRouteById,
  createRoute,
  updateRoute,
  deleteRoute,
} from "../controllers/routeController.js";

const router = express.Router();

// GET /api/routes - Returns all routes
router.get("/", getAllRoutes);

// GET /api/routes/:id - Returns one route by its MongoDB _id
router.get("/:id", getRouteById);

// POST /api/routes - Creates a new route entry
router.post("/", createRoute);

// PUT /api/routes/:id - Updates an existing route by _id
router.put("/:id", updateRoute);

// DELETE /api/routes/:id - Removes a route by _id
router.delete("/:id", deleteRoute);

export default router;
