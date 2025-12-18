import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cors from 'cors';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { connectDB } from './config/db.js';
import buildingRoutes from './routes/buildings.js';
import tourRoutes from './routes/tours.js';
import authRoutes from './routes/auth.js';
import uploadRoutes from './routes/upload.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
const PORT = 3000;

// Connect to MongoDB
connectDB();


app.use(cors({ origin: 'http://localhost:5173' }));

app.use(express.json({ type: (req) => {
  return req.headers['content-type'] && !req.headers['content-type'].includes('multipart/form-data');
}}));


app.use('/assets/uploads', express.static(join(__dirname, '../frontend/public/assets/uploads')));

// Routes

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    message: 'Backend is running'
  });
});

// Building CRUD routes
app.use('/api/buildings', buildingRoutes);

// Tour CRUD routes
app.use('/api/tours', tourRoutes);

// Authentication routes (signup, login)
app.use('/auth', authRoutes);

// File upload routes
app.use('/api/upload', uploadRoutes);

// Start server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

