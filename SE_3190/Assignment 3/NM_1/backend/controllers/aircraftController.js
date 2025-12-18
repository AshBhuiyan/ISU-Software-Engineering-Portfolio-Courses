import { getDB } from "../config/db.js";
import { ObjectId } from "mongodb";

// GET /api/aircraft - Fetch all aircraft from the database
export async function getAllAircraft(req, res) {
  try {
    const db = getDB();
    const aircraft = await db.collection("aircraft").find({}).toArray();
    
    return res.status(200).json(aircraft);
  } catch (err) {
    console.error("Error fetching aircraft:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// GET /api/aircraft/:id - Fetch a single aircraft by its ObjectId
export async function getAircraftById(req, res) {
  try {
    const db = getDB();
    const { id } = req.params;

    // Validate ObjectId format
    if (!ObjectId.isValid(id)) {
      return res.status(400).json({ error: "Invalid aircraft ID format" });
    }

    const aircraft = await db.collection("aircraft").findOne({ _id: new ObjectId(id) });

    if (!aircraft) {
      return res.status(404).json({ error: "Aircraft not found" });
    }

    return res.status(200).json(aircraft);
  } catch (err) {
    console.error("Error fetching aircraft by ID:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// POST /api/aircraft - Create a new aircraft
export async function createAircraft(req, res) {
  try {
    const db = getDB();
    const aircraftData = req.body;

    // Validate required fields
    if (!aircraftData.serialNumber || !aircraftData.model || !aircraftData.airline) {
      return res.status(400).json({ 
        error: "Bad Request", 
        message: "Missing required fields: serialNumber, model, airline" 
      });
    }

    // Check if aircraft with same serial number already exists
    const existingAircraft = await db.collection("aircraft").findOne({ 
      serialNumber: aircraftData.serialNumber 
    });
    if (existingAircraft) {
      return res.status(400).json({ 
        error: "Bad Request", 
        message: "Aircraft with this serial number already exists" 
      });
    }

    // Ensure assignedRoutes is an array if provided
    if (aircraftData.assignedRoutes && !Array.isArray(aircraftData.assignedRoutes)) {
      aircraftData.assignedRoutes = [];
    }

    const result = await db.collection("aircraft").insertOne(aircraftData);
    const newAircraft = await db.collection("aircraft").findOne({ _id: result.insertedId });

    return res.status(201).json(newAircraft);
  } catch (err) {
    console.error("Error creating aircraft:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// PUT /api/aircraft/:id - Update an existing aircraft
export async function updateAircraft(req, res) {
  try {
    const db = getDB();
    const { id } = req.params;
    const updateData = req.body;

    // Validate ObjectId format
    if (!ObjectId.isValid(id)) {
      return res.status(400).json({ error: "Invalid aircraft ID format" });
    }

    // Check if aircraft exists
    const existingAircraft = await db.collection("aircraft").findOne({ _id: new ObjectId(id) });
    if (!existingAircraft) {
      return res.status(404).json({ error: "Aircraft not found" });
    }

    // Remove _id from update data if present (cannot update _id)
    delete updateData._id;

    const result = await db.collection("aircraft").updateOne(
      { _id: new ObjectId(id) },
      { $set: updateData }
    );

    if (result.matchedCount === 0) {
      return res.status(404).json({ error: "Aircraft not found" });
    }

    // Fetch and return updated aircraft
    const updatedAircraft = await db.collection("aircraft").findOne({ _id: new ObjectId(id) });
    return res.status(200).json(updatedAircraft);
  } catch (err) {
    console.error("Error updating aircraft:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// DELETE /api/aircraft/:id - Delete an aircraft
export async function deleteAircraft(req, res) {
  try {
    const db = getDB();
    const { id } = req.params;

    // Validate ObjectId format
    if (!ObjectId.isValid(id)) {
      return res.status(400).json({ error: "Invalid aircraft ID format" });
    }

    // Get aircraft info before deletion for response
    const aircraft = await db.collection("aircraft").findOne({ _id: new ObjectId(id) });
    
    if (!aircraft) {
      return res.status(404).json({ error: "Aircraft not found" });
    }

    const result = await db.collection("aircraft").deleteOne({ _id: new ObjectId(id) });

    if (result.deletedCount === 0) {
      return res.status(404).json({ error: "Aircraft not found" });
    }

    return res.status(200).json({ 
      message: "Aircraft deleted successfully",
      deletedId: id,
      serialNumber: aircraft.serialNumber 
    });
  } catch (err) {
    console.error("Error deleting aircraft:", err);
    return res.status(500).json({ error: "Internal server error", message: err.message });
  }
}

// GET /api/aircraft/assigned-routes/:model - Get all routes assigned to aircraft of a specific model
export async function getAssignedRoutesByAircraft(req, res) {
  try {
    const db = getDB();
    const model = req.params.model;

    if (!model || typeof model !== "string") {
      return res.status(400).json({
        message: "Model parameter is required and must be a string",
      });
    }

    const aircraftList = await db
      .collection("aircraft")
      .find({ model })
      .project({ assignedRoutes: 1, serialNumber: 1 })
      .toArray();

    if (!aircraftList || aircraftList.length === 0) {
      return res.status(404).json({
        message: "No routes found for this model",
      });
    }

    const routes = aircraftList.flatMap((aircraft) => aircraft.assignedRoutes || []);

    return res.status(200).json({
      model,
      aircraftCount: aircraftList.length,
      routes,
    });
  } catch (error) {
    console.error("Error fetching assigned routes by aircraft model:", error);

    return res.status(500).json({
      message: "Internal server error while fetching assigned routes",
      error: error.message,
    });
  }
}
