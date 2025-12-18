import mongoose from 'mongoose';

/**
 * User Model
 * Represents a user account (admin or student)
 * Used for authentication and authorization
 */
const userSchema = new mongoose.Schema({
  // Full name
  name: {
    type: String,
    required: true,
    trim: true
  },
  
  // Email address 
  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    trim: true,
    match: [/^\S+@\S+\.\S+$/, 'Please provide a valid email address']
  },
  
  // Password 

  password: {
    type: String,
    required: true,
    minlength: 8
  },
  
  // User role: 
  role: {
    type: String,
    required: true,
    enum: ['admin', 'user'],
    default: 'user'
  }
}, {
  timestamps: true // Adds createdAt and updatedAt fields
});

// Index for faster email lookups
userSchema.index({ email: 1 });

// Method to return user without password 
userSchema.methods.toJSON = function() {
  const userObject = this.toObject();
  delete userObject.password;
  return userObject;
};

const User = mongoose.model('User', userSchema);

export default User;
