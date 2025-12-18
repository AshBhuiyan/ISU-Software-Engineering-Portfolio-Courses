import express from 'express';
import Building from '../models/Building.js';

const router = express.Router();

/**
 * GET /api/buildings
 * Get all buildings
 * 
 */
router.get('/', async (req, res) => {
  try {
    const buildings = await Building.find().sort({ name: 1 });
    res.json({
      success: true,
      buildings
    });
  } catch (error) {
    console.error('Error fetching buildings:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch buildings',
      error: error.message
    });
  }
});

/**
 * GET /api/buildings/:id
 * Get a single building by ID
 * 
 */
router.get('/:id', async (req, res) => {
  try {
    const building = await Building.findOne({ id: req.params.id });
    
    if (!building) {
      return res.status(404).json({
        success: false,
        message: 'Building not found'
      });
    }
    
    res.json({
      success: true,
      building
    });
  } catch (error) {
    console.error('Error fetching building:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch building',
      error: error.message
    });
  }
});

/**
 * POST /api/buildings
 * Create a new building
 */
router.post('/', async (req, res) => {
  try {
    const {
      id,
      name,
      code,
      type,
      departments,
      description,
      hours,
      capacity,
      yearBuilt,
      floors,
      tags,
      coordinates
    } = req.body;

    // Validate required fields
    if (!id || !name || !code || !type || !description || !hours || !capacity || !yearBuilt || !floors) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: id, name, code, type, description, hours, capacity, yearBuilt, floors'
      });
    }

    // Check if building with this ID already exists
    const existing = await Building.findOne({ id: id.toLowerCase().trim() });
    if (existing) {
      return res.status(409).json({
        success: false,
        message: 'Building with this ID already exists'
      });
    }

    // Default hero image for new buildings
    const DEFAULT_HERO_IMAGE = '/assets/uploads/Iowa_State_Cyclones_logo.svg-1765491050320-441751975.png';
    // Default floor plan image
    const DEFAULT_FLOOR_PLAN = '/assets/images/Iowa_State_Cyclones_logo.svg.png';

    // Create default floor plans array 
    const defaultFloorPlans = Array(parseInt(floors)).fill(DEFAULT_FLOOR_PLAN);

    // Create new building
    const building = new Building({
      id: id.toLowerCase().trim(),
      name: name.trim(),
      code: code.trim().toUpperCase(),
      type,
      departments: departments || [],
      description: description.trim(),
      hours: hours.trim(),
      capacity: capacity.trim(),
      yearBuilt: yearBuilt.toString().trim(),
      floors: parseInt(floors),
      tags: tags || [],
      coordinates: coordinates || null,
      image: DEFAULT_HERO_IMAGE, // Set default hero image
      floorPlans: defaultFloorPlans // Set default floor plans 
    });

    const savedBuilding = await building.save();
    
    res.status(201).json({
      success: true,
      building: savedBuilding
    });
  } catch (error) {
    console.error('Error creating building:', error);
    
    // Handle validation errors
    if (error.name === 'ValidationError') {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: error.message
      });
    }
    
    res.status(500).json({
      success: false,
      message: 'Failed to create building',
      error: error.message
    });
  }
});

/**
 * PUT /api/buildings/:id
 * Update an existing building
 */
router.put('/:id', async (req, res) => {
  try {
    const building = await Building.findOne({ id: req.params.id });
    
    if (!building) {
      return res.status(404).json({
        success: false,
        message: 'Building not found'
      });
    }

    // Update only provided fields
    const {
      name,
      code,
      type,
      departments,
      description,
      hours,
      capacity,
      yearBuilt,
      floors,
      tags,
      coordinates,
      image,
      gallery,
      video,
      floorPlans
    } = req.body;

    if (name !== undefined) building.name = name.trim();
    if (code !== undefined) building.code = code.trim().toUpperCase();
    if (type !== undefined) building.type = type;
    if (departments !== undefined) building.departments = departments;
    if (description !== undefined) building.description = description.trim();
    if (hours !== undefined) building.hours = hours.trim();
    if (capacity !== undefined) building.capacity = capacity.trim();
    if (yearBuilt !== undefined) building.yearBuilt = yearBuilt.toString().trim();
    if (floors !== undefined) building.floors = parseInt(floors);
    if (tags !== undefined) building.tags = tags;
    if (coordinates !== undefined) building.coordinates = coordinates;
    if (image !== undefined) building.image = image;
    if (gallery !== undefined) building.gallery = gallery;
    if (video !== undefined) building.video = video;
    if (floorPlans !== undefined) building.floorPlans = floorPlans;

    const updatedBuilding = await building.save();
    
    res.json({
      success: true,
      building: updatedBuilding
    });
  } catch (error) {
    console.error('Error updating building:', error);
    
    if (error.name === 'ValidationError') {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: error.message
      });
    }
    
    res.status(500).json({
      success: false,
      message: 'Failed to update building',
      error: error.message
    });
  }
});

/**
 * DELETE /api/buildings/:id
 * Delete a building
 */
router.delete('/:id', async (req, res) => {
  try {
    const building = await Building.findOne({ id: req.params.id });
    
    if (!building) {
      return res.status(404).json({
        success: false,
        message: 'Building not found'
      });
    }

    await Building.deleteOne({ id: req.params.id });
    
    res.json({
      success: true,
      message: 'Building deleted successfully'
    });
  } catch (error) {
    console.error('Error deleting building:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to delete building',
      error: error.message
    });
  }
});

export default router;
