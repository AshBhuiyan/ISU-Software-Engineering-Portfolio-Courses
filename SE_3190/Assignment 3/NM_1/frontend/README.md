# SkyValor Frontend

The SkyValor frontend is a fully built airline-operations interface created using **React**, **Vite**, and **TailwindCSS**. Every page is already wired to fetch data from your backend once implemented. Form modals, selection states, update dialogs, confirmation flows, and deletion prompts are all functional — the backend responses simply activate them.

The frontend is modeled after real airline dashboards and is designed to work seamlessly with the SkyValor API.

---

## What the Frontend Already Includes

The interface ships with complete pages, routing, UI logic, and modal workflows. Once the backend API is live, the UI updates instantly with no structural changes required.

---

## Pages Included in the UI

### **Home Dashboard**

- High-level operations summary with live flight markers.
- Quick navigation buttons that route to **/routes** and **/aircraft**.

---

### **Routes Page**

- Fetches and displays **all routes** from `GET /api/routes`.
- SkyValor routes and Partner Airline routes are visually grouped.
- Each route is rendered using `RouteCard.jsx`.
- Add Route and Update Route open a `RouteForm` modal (backend required).
- The UI sorts SkyValor flights first, then external operators.
- Deleting a route triggers a confirmation dialog before removal.

---

### **Route Details View**

- Fetches a specific route via: `GET /api/routes/:id`
- Aircraft are grouped dynamically by airline.
- Airline logos render automatically (SkyValor, United, Air India, Emirates, Etihad, etc).
- Create and Update operations use the `AircraftForm` modal.

---

### **Aircraft Details View**

- Shows tail number (serial number), status, operator, and assigned routes.
- Delete triggers a confirmation dialog — requires backend DELETE to work.
- Assigned routes auto-render with airport names and codes.

---

### **AircraftForm Modal**

- Used for both Create & Update operations.
- Supports automatic airline prefix mapping (UA → United, AI → Air India, etc).
- Pulls available aircraft models from: `GET /api/aircraftsInMarket`
- Assign-Route UI allows selecting **FROM → TO** airport pairs.

---

### **RouteForm Modal**

- Builds a flight number using prefix + numeric input (ex: UA683, SV240).
- Automatically computes distance using the haversine formula.
- Search-by-Flight-ID autofills the edit form using backend lookup.
- Edit mode loads previous values correctly once backend responds.

---

### **Confirmation Page**

- Triggered after Create / Update / Delete operations.
- Reads navigation state that contains payload returned by your backend.
- Works only if your API returns JSON bodies in the expected format.

---

## Important Note

**No React changes are required.**

Once your backend responds correctly, the UI will automatically:

- Load data
- Update and edit entries
- Delete routes or aircraft
- Show confirmation pages
- Reflect backend state visually
