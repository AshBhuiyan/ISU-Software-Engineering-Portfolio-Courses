import mongoose from 'mongoose';

/**
 * Tour Model
 * Represents a saved campus tour created by a user
 * This is our second CRUD entity and also serves as the Advanced Feature
 * Tours contain an ordered list of building IDs that the user wants to visit
 */
const tourSchema = new mongoose.Schema({
  // Tour name 
  name: {
    type: String,
    required: true,
    trim: true,
    maxlength: 200
  },
  
  // description
  description: {
    type: String,
    trim: true,
    maxlength: 1000,
    default: ''
  },
  
  // Reference to the user who created this tour
  owner: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  
  // Ordered list of building IDs 
 
  buildingIds: {
    type: [String], // Array of building.id strings 
    required: true,
    validate: {
      validator: function(v) {
        return Array.isArray(v) && v.length > 0;
      },
      message: 'Tour must have at least one building'
    }
  },
  
  // Estimated duration in minutes
  estimatedDuration: {
    type: Number,
    min: 0,
    default: null
  },
  
  // Tags for categorization 
  tags: {
    type: [String],
    default: [],
    trim: true
  }
}, {
  timestamps: true // Adds createdAt and updatedAt fields
});


tourSchema.index({ owner: 1, createdAt: -1 }); // Get user's tours, newest first
tourSchema.index({ name: 'text', description: 'text' }); // Text search


tourSchema.virtual('stopCount').get(function() {
  return this.buildingIds ? this.buildingIds.length : 0;
});


tourSchema.set('toJSON', { virtuals: true });

const Tour = mongoose.model('Tour', tourSchema);

export default Tour;
