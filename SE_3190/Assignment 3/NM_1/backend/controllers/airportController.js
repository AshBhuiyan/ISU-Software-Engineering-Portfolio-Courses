import { getDB } from "../config/db.js";

export async function getAllAirport(req, res) {
  try {
    const db = getDB();
    const airports = await db.collection("airports").find().toArray();

    // Explicit status & JSON as per assignment guidelines
    return res.status(200).json(airports);
  } catch (error) {
    console.error("Error fetching airports:", error);

    return res.status(500).json({
      message: "Internal server error while fetching airports",
      error: error.message,
    });
  }
}
