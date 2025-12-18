import { getDB } from "../config/db.js";
import { ObjectId } from "mongodb";

// GET /api/routes - Fetch all routes from the database
export async function getAllRoutes(req, res) {
  try {
    const db = getDB();
    const routes = await db.collection("routes").find({}).toArray();
    
    return res.status(200).json(routes);
  } catch (err) {
    console.error("Error fetching routes:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// GET /api/routes/:id - Fetch a single route by its ObjectId
export async function getRouteById(req, res) {
  try {
    const db = getDB();
    const { id } = req.params;

    // Validate ObjectId format
    if (!ObjectId.isValid(id)) {
      return res.status(400).json({ error: "Invalid route ID format" });
    }

    const route = await db.collection("routes").findOne({ _id: new ObjectId(id) });

    if (!route) {
      return res.status(404).json({ error: "Route not found" });
    }

    return res.status(200).json(route);
  } catch (err) {
    console.error("Error fetching route by ID:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// POST /api/routes - Create a new route
export async function createRoute(req, res) {
  try {
    const db = getDB();
    const routeData = req.body;

    // Validate required fields
    if (!routeData.id || !routeData.airline || !routeData.from || !routeData.to) {
      return res.status(400).json({ 
        error: "Bad Request", 
        message: "Missing required fields: id, airline, from, to" 
      });
    }

    // Check if route with same id already exists
    const existingRoute = await db.collection("routes").findOne({ id: routeData.id });
    if (existingRoute) {
      return res.status(400).json({ 
        error: "Bad Request", 
        message: "Route with this ID already exists" 
      });
    }

    const result = await db.collection("routes").insertOne(routeData);
    const newRoute = await db.collection("routes").findOne({ _id: result.insertedId });

    return res.status(201).json(newRoute);
  } catch (err) {
    console.error("Error creating route:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// PUT /api/routes/:id - Update an existing route
export async function updateRoute(req, res) {
  try {
    const db = getDB();
    const { id } = req.params;
    const updateData = req.body;

    // Validate ObjectId format
    if (!ObjectId.isValid(id)) {
      return res.status(400).json({ error: "Invalid route ID format" });
    }

    // Check if route exists
    const existingRoute = await db.collection("routes").findOne({ _id: new ObjectId(id) });
    if (!existingRoute) {
      return res.status(404).json({ error: "Route not found" });
    }

    // Remove _id from update data if present (cannot update _id)
    delete updateData._id;

    const result = await db.collection("routes").updateOne(
      { _id: new ObjectId(id) },
      { $set: updateData }
    );

    if (result.matchedCount === 0) {
      return res.status(404).json({ error: "Route not found" });
    }

    // Fetch and return updated route
    const updatedRoute = await db.collection("routes").findOne({ _id: new ObjectId(id) });
    return res.status(200).json(updatedRoute);
  } catch (err) {
    console.error("Error updating route:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// DELETE /api/routes/:id - Delete a route
export async function deleteRoute(req, res) {
  try {
    const db = getDB();
    const { id } = req.params;

    // Validate ObjectId format
    if (!ObjectId.isValid(id)) {
      return res.status(400).json({ error: "Invalid route ID format" });
    }

    // Get route info before deletion for response
    const route = await db.collection("routes").findOne({ _id: new ObjectId(id) });
    
    if (!route) {
      return res.status(404).json({ error: "Route not found" });
    }

    const result = await db.collection("routes").deleteOne({ _id: new ObjectId(id) });

    if (result.deletedCount === 0) {
      return res.status(404).json({ error: "Route not found" });
    }

    return res.status(200).json({ 
      message: "Route deleted successfully",
      routeId: route.id,
      deletedId: id 
    });
  } catch (err) {
    console.error("Error deleting route:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}
