/**
 * MongoDB connection logic.
 * This file:
 * 1. Loads MONGO_URI from environment variables.
 * 2. Connects to MongoDB using MongoClient.
 * 3. Selects the "skyvalor" database.
 * 4. Exports connectDB() and getDB() for use across the app.
 */

import { MongoClient } from "mongodb";
import dotenv from "dotenv";
dotenv.config();

let db;
let client;

export async function connectDB() {
  try {
    const MONGO_URI = process.env.MONGO_URI;
    
    if (!MONGO_URI) {
      throw new Error("MONGO_URI is not defined in environment variables");
    }

    client = new MongoClient(MONGO_URI);
    await client.connect();
    
    db = client.db("skyvalor");
    
    console.log("✅ Connected to MongoDB - skyvalor database");
    return db;
  } catch (err) {
    console.error("❌ MongoDB connection error:", err.message);
    process.exit(1);
  }
}

export function getDB() {
  if (!db) {
    throw new Error("Database not initialized. Call connectDB() first.");
  }
  return db;
}
