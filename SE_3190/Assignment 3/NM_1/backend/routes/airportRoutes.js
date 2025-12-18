import express from "express";
import { getAllAirport } from "../controllers/airportController.js";

const router = express.Router();

router.get("/", getAllAirport);

export default router;
