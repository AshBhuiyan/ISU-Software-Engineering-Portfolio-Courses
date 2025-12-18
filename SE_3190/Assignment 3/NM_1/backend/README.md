# SkyValor Airlines – Backend (Assignment 3, SE/COM S 3190)

This backend powers the **SkyValor Airlines** React frontend for Assignment 3 (Fall 2025).  
It exposes a REST API for managing flight **routes**, **aircraft**, **airports**, and **aircraft models available in the market**.

The frontend is fully provided and calls these APIs to display real data from MongoDB.

---

## 1. Tech Stack

- **Node.js + Express**
- **MongoDB** (using the official `mongodb` driver)
- **dotenv** for environment variables
- **CORS** for local dev with the Vite frontend

Database name: **`skyvalor`** (required by assignment).

---

## 2. Setup & Installation

1. Clone the repository and go to the backend folder:

   ```bash
   cd backend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Create a `.env` file in `backend/` with at least:

   ```env
   MONGO_URI=mongodb://localhost:27017
   ```

   The backend code connects to the `skyvalor` database name internally.

4. Import seed data into MongoDB using MongoDB Compass (as per assignment):

   * `frontend/src/data/routes.json`    → `routes` collection
   * `frontend/src/data/aircraft.json`  → `aircraft` collection
   * `frontend/src/data/airports.json`  → `airports` collection
   * `frontend/src/data/aircraftsInMarket.json` → `aircraftsInMarket` collection

5. Start the backend server:

   ```bash
   npm start
   ```

   The API runs at: `http://localhost:8081`.

6. Start the frontend (from `frontend/` folder) and point it to `http://localhost:8081/api`.

---

## 3. Folder Structure (Backend)

```text
backend/
  config/
    db.js                 # Mongo connection helpers (connectDB, getDB)
  controllers/
    routeController.js    # Routes CRUD handlers
    aircraftController.js # Aircraft CRUD + assigned routes handlers
    airportController.js  # Airports GET handler
  routes/
    routeRoutes.js        # /api/routes endpoints
    aircraftRoutes.js     # /api/aircraft & assigned-routes endpoints
    airportRoutes.js      # /api/airports endpoint
  server.js               # Express app, middleware, route mounting
  package.json
  README.md               # This document
  .env                    # Contains MONGO_URI
```

---

## 4. API Documentation

Base URL:

```text
http://localhost:8081/api
```

### 4.1 Routes API

#### GET `/api/routes`

* **Description:** Get all routes.
* **Response 200:**

  ```json
  [
    {
      "_id": "65f...",
      "id": "SV243",
      "airline": "SkyValor",
      "from": { "code": "JFK", "name": "John F. Kennedy International Airport" },
      "to": { "code": "LHR", "name": "London Heathrow Airport" },
      "distance_miles": 3450,
      "aircraft": "SV-787-1",
      "passengers": 220,
      "...": "other fields as in routes.json"
    },
    ...
  ]
  ```

#### GET `/api/routes/:id`

* **Description:** Get a single route by Mongo `_id`.
* **Params:** `id` – MongoDB ObjectId string.
* **Responses:**

  * `200` – route object.
  * `400` – invalid ObjectId format.
  * `404` – route not found.

#### POST `/api/routes`

* **Description:** Create a new route.

* **Body (JSON):** Must include at least:

  ```json
  {
    "id": "SV999",
    "airline": "SkyValor",
    "from": { "code": "ORD", "name": "Chicago O'Hare International Airport" },
    "to": { "code": "SFO", "name": "San Francisco International Airport" },
    "distance_miles": 1846,
    "aircraft": "SV-737-9",
    "passengers": 150
  }
  ```

* **Responses:**

  * `201` – created route.
  * `400` – missing required fields or duplicate `id`.
  * `500` – internal server error.

#### PUT `/api/routes/:id`

* **Description:** Update an existing route.
* **Body (JSON):** Any updatable fields (e.g., `aircraft`, `status`, etc.).
* **Responses:**

  * `200` – updated route.
  * `400` – invalid ObjectId.
  * `404` – route not found.
  * `500` – internal server error.

#### DELETE `/api/routes/:id`

* **Description:** Delete a route by `_id`.
* **Responses:**

  * `200` – JSON like:

    ```json
    {
      "message": "Route deleted successfully",
      "routeId": "SV243",
      "deletedId": "65f..."
    }
    ```

  * `400` – invalid ObjectId.
  * `404` – route not found.
  * `500` – internal server error.

#### GET `/api/routes/search/:flightId`

* **Description:** Search a route by flight ID (e.g. `SV243`, `UA990`).
* **Responses:**

  * `200` – route object matching `id`.
  * `404` – `{ "error": "Route not found" }`.
  * `500` – internal server error.

---

### 4.2 Aircraft API

#### GET `/api/aircraft`

* **Description:** Get all aircraft.
* **Response 200:** Array of aircraft documents.

#### GET `/api/aircraft/:id`

* **Description:** Get aircraft by Mongo `_id`.
* **Responses:**

  * `200` – aircraft object.
  * `400` – invalid ObjectId.
  * `404` – not found.
  * `500` – internal server error.

#### POST `/api/aircraft`

* **Description:** Create a new aircraft.
* **Required fields in body:**

  * `serialNumber` (unique)
  * `model`
  * `airline`

* **Responses:**

  * `201` – created aircraft.
  * `400` – missing required fields or duplicate serial number.
  * `500` – internal server error.

#### PUT `/api/aircraft/:id`

* **Description:** Update an existing aircraft by `_id`.
* **Responses:**

  * `200` – updated aircraft.
  * `400` – invalid ObjectId.
  * `404` – aircraft not found.
  * `500` – internal server error.

#### DELETE `/api/aircraft/:id`

* **Description:** Delete aircraft by `_id`.
* **Responses:**

  * `200` – JSON like:

    ```json
    {
      "message": "Aircraft deleted successfully",
      "deletedId": "65f...",
      "serialNumber": "SV-787-1"
    }
    ```

  * `400` – invalid ObjectId.
  * `404` – not found.
  * `500` – internal server error.

#### GET `/api/aircraft/search/:serial`

* **Description:** Find an aircraft by its `serialNumber`.
* **Responses:**

  * `200` – aircraft object.
  * `404` – `{ "error": "Aircraft not found" }`.
  * `500` – internal server error.

#### GET `/api/aircraft/assigned-routes/:model`

* **Description:** Get all routes assigned to aircraft of a specific model.
* **Responses:**

  * `200` – JSON:

    ```json
    {
      "model": "Boeing 787-9",
      "aircraftCount": 3,
      "routes": [
        { "from": "...", "to": "...", "flightId": "SV243", "...": "..." },
        ...
      ]
    }
    ```

  * `400` – missing/invalid model parameter.
  * `404` – no routes found for that model.
  * `500` – internal server error.

---

### 4.3 Airports & Market API

#### GET `/api/airports`

* **Description:** Get all airports (used for dropdowns and distance calculations).
* **Responses:**

  * `200` – array of airports.
  * `500` – internal server error.

#### GET `/api/aircraftsInMarket`

* **Description:** Get list of aircraft models available in the market.
* **Responses:**

  * `200` – array of market aircraft documents.
  * `500` – internal server error.

---

## 5. Error Handling & Status Codes

The backend uses consistent HTTP codes:

* `200 OK` – Successful read or update.
* `201 Created` – Successful creation.
* `400 Bad Request` – Missing or invalid input (e.g., invalid ObjectId, missing required fields).
* `404 Not Found` – Document or search result not found.
* `500 Internal Server Error` – Unexpected server or database errors.

All API responses are JSON, including errors:

```json
{
  "message": "Human-readable description",
  "error": "Optional technical error message"
}
```

---

## 6. Notes

* Frontend code is provided and not modified; all behavior comes from this backend.
* Database name is **always** `skyvalor`.
* All seed data fields from the JSON files are preserved in MongoDB.
