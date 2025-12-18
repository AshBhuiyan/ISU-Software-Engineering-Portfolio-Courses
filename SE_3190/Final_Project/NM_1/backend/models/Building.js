import mongoose from 'mongoose';

/**
 * Building Model
 * Represents a campus building with all relevant information
 * Used by CampusMap, StudentDashboard, and Admin building management
 */
const buildingSchema = new mongoose.Schema({
  
  id: {
    type: String,
    required: true,
    unique: true,
    trim: true,
    lowercase: true
  },
  
  // Building name 
  name: {
    type: String,
    required: true,
    trim: true
  },
  
  // Building code 
  code: {
    type: String,
    required: true,
    trim: true,
    uppercase: true
  },
  
  // Building type 
  type: {
    type: String,
    required: true,
    enum: ['Academic', 'Administration', 'Student Life', 'Athletics', 'Residence', 'Landmark'],
    trim: true
  },
  
  // List of departments/services in the building
  departments: {
    type: [String],
    default: [],
    trim: true
  },
  
  // Detailed description
  description: {
    type: String,
    required: true,
    trim: true
  },
  
  // Operating hours
  hours: {
    type: String,
    required: true,
    trim: true
  },
  
  // Capacity information
  capacity: {
    type: String,
    required: true,
    trim: true
  },
  
  // Year the building was constructed
  yearBuilt: {
    type: String,
    required: true,
    trim: true
  },
  
  // Number of floors
  floors: {
    type: Number,
    required: true,
    min: 1
  },
  
  //Tags for filtering/searching 
  tags: {
    type: [String],
    default: [],
    trim: true
  },
  
  // Coordinates for map 
  coordinates: {
    x: { type: Number, default: null },
    y: { type: Number, default: null }
  },
  
  // Hero image URL
  image: {
    type: String,
    default: null,
    trim: true
  },
  
  // Gallery images array
  gallery: {
    type: [String],
    default: []
  },
  
  // Video URL
  video: {
    type: String,
    default: null,
    trim: true
  },
  
  // Floor plan images array 
  floorPlans: {
    type: [String],
    default: [],
    trim: true
  }
}, {
  timestamps: true // Adds createdAt and updatedAt fields
});

// Index for faster searches
buildingSchema.index({ name: 'text', description: 'text', code: 'text' });
buildingSchema.index({ type: 1 });
buildingSchema.index({ id: 1 });

const Building = mongoose.model('Building', buildingSchema);

export default Building;
