import dotenv from 'dotenv';
dotenv.config();

import { connectDB } from '../config/db.js';
import User from '../models/User.js';

// Default users to seed 
const defaultUsers = [
  {
    name: 'Admin User',
    email: 'admin@iastate.edu',
    password: 'admin123',
    role: 'admin'
  },
  {
    name: 'John Student',
    email: 'student1@iastate.edu',
    password: 'student123',
    role: 'user'
  },
  {
    name: 'Jane Student',
    email: 'student2@iastate.edu',
    password: 'student123',
    role: 'user'
  }
];

async function seedUsers() {
  try {
    await connectDB();
    
    console.log('🌱 Seeding users...');
    
    let created = 0;
    let skipped = 0;
    
    for (const userData of defaultUsers) {
      const existing = await User.findOne({ email: userData.email });
      
      if (existing) {
        console.log(`⏭️  Skipping ${userData.email} (already exists)`);
        skipped++;
      } else {
        const user = new User(userData);
        await user.save();
        console.log(`✅ Created ${userData.email} (${userData.role})`);
        created++;
      }
    }
    
    console.log(`\n✨ Seeding complete!`);
    console.log(`   Created: ${created}`);
    console.log(`   Skipped: ${skipped}`);
    console.log(`   Total: ${defaultUsers.length}`);
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Error seeding users:', error);
    process.exit(1);
  }
}

seedUsers();
