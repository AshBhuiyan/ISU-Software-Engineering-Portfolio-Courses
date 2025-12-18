import mongoose from 'mongoose';

/**
 * Connect to MongoDB database
 * Reads connection string from process.env.MONGO_URI
 * Logs success or failure
 */
export const connectDB = async () => {
  try {
    const mongoURI = process.env.MONGO_URI;
    
    if (!mongoURI) {
      console.error('❌ MONGO_URI environment variable is not set');
      console.log('💡 Please create a .env file in the backend directory with: MONGO_URI=your_connection_string');
      process.exit(1);
    }

    const conn = await mongoose.connect(mongoURI);
    
    console.log(`✅ MongoDB Connected: ${conn.connection.host}`);
    console.log(`📊 Database: ${conn.connection.name}`);
  } catch (error) {
    console.error('❌ MongoDB connection error:', error.message);
    process.exit(1);
  }
};
