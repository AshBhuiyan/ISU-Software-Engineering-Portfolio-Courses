import express from 'express';
import Tour from '../models/Tour.js';
import User from '../models/User.js';

const router = express.Router();

/**
 * GET /api/tours
 * Get all tours for the logged-in user
 */
router.get('/', async (req, res) => {
  try {
    const { ownerEmail } = req.query;

    if (!ownerEmail) {
      return res.status(400).json({
        success: false,
        message: 'ownerEmail query parameter is required'
      });
    }

    // Find user by email
    let user = await User.findOne({ email: ownerEmail.toLowerCase() });
    
    if (!user) {
      // Return empty array if user doesn't exist
      return res.json({
        success: true,
        tours: []
      });
    }

    // Get all tours for this user, sorted by newest first
    const tours = await Tour.find({ owner: user._id })
      .sort({ createdAt: -1 })
      .populate('owner', 'name email'); // Populate owner info

    res.json({
      success: true,
      tours
    });
  } catch (error) {
    console.error('Error fetching tours:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch tours',
      error: error.message
    });
  }
});

/**
 * GET /api/tours/:id
 * Get a single tour by ID
 */
router.get('/:id', async (req, res) => {
  try {
    const tour = await Tour.findById(req.params.id)
      .populate('owner', 'name email');

    if (!tour) {
      return res.status(404).json({
        success: false,
        message: 'Tour not found'
      });
    }

    res.json({
      success: true,
      tour
    });
  } catch (error) {
    console.error('Error fetching tour:', error);
    
    if (error.name === 'CastError') {
      return res.status(400).json({
        success: false,
        message: 'Invalid tour ID'
      });
    }

    res.status(500).json({
      success: false,
      message: 'Failed to fetch tour',
      error: error.message
    });
  }
});

/**
 * POST /api/tours
 * Create a new tour
 */
router.post('/', async (req, res) => {
  try {
    const {
      name,
      description,
      buildingIds,
      ownerEmail,
      estimatedDuration,
      tags
    } = req.body;

    // Validate required fields
    if (!name || !buildingIds || !ownerEmail) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: name, buildingIds, ownerEmail'
      });
    }

    if (!Array.isArray(buildingIds) || buildingIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'buildingIds must be a non-empty array'
      });
    }

    // Find or create user by email
    let user = await User.findOne({ email: ownerEmail.toLowerCase() });
    
    if (!user) {
      // Create a temporary user 
      user = new User({
        name: ownerEmail.split('@')[0], // Use email prefix as name
        email: ownerEmail.toLowerCase(),
        password: 'temp', // Temporary password
        role: 'user'
      });
      await user.save();
    }

    // Create new tour
    const tour = new Tour({
      name: name.trim(),
      description: description ? description.trim() : '',
      owner: user._id,
      buildingIds: buildingIds.map(id => id.trim().toLowerCase()),
      estimatedDuration: estimatedDuration || null,
      tags: tags || []
    });

    const savedTour = await tour.save();
    
    // Populate owner info before returning
    await savedTour.populate('owner', 'name email');

    res.status(201).json({
      success: true,
      tour: savedTour
    });
  } catch (error) {
    console.error('Error creating tour:', error);
    
    if (error.name === 'ValidationError') {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: error.message
      });
    }
    
    res.status(500).json({
      success: false,
      message: 'Failed to create tour',
      error: error.message
    });
  }
});

/**
 * PUT /api/tours/:id
 * Update an existing tour
 */
router.put('/:id', async (req, res) => {
  try {
    const tour = await Tour.findById(req.params.id);
    
    if (!tour) {
      return res.status(404).json({
        success: false,
        message: 'Tour not found'
      });
    }

    // Update only provided fields
    const {
      name,
      description,
      buildingIds,
      estimatedDuration,
      tags
    } = req.body;

    if (name !== undefined) tour.name = name.trim();
    if (description !== undefined) tour.description = description.trim();
    if (buildingIds !== undefined) {
      if (!Array.isArray(buildingIds) || buildingIds.length === 0) {
        return res.status(400).json({
          success: false,
          message: 'buildingIds must be a non-empty array'
        });
      }
      tour.buildingIds = buildingIds.map(id => id.trim().toLowerCase());
    }
    if (estimatedDuration !== undefined) tour.estimatedDuration = estimatedDuration;
    if (tags !== undefined) tour.tags = tags;

    const updatedTour = await tour.save();
    await updatedTour.populate('owner', 'name email');
    
    res.json({
      success: true,
      tour: updatedTour
    });
  } catch (error) {
    console.error('Error updating tour:', error);
    
    if (error.name === 'ValidationError') {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: error.message
      });
    }
    
    res.status(500).json({
      success: false,
      message: 'Failed to update tour',
      error: error.message
    });
  }
});

/**
 * DELETE /api/tours/:id
 * Delete a tour
 */
router.delete('/:id', async (req, res) => {
  try {
    const tour = await Tour.findById(req.params.id);
    
    if (!tour) {
      return res.status(404).json({
        success: false,
        message: 'Tour not found'
      });
    }

    await Tour.findByIdAndDelete(req.params.id);
    
    res.json({
      success: true,
      message: 'Tour deleted successfully'
    });
  } catch (error) {
    console.error('Error deleting tour:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to delete tour',
      error: error.message
    });
  }
});

export default router;
