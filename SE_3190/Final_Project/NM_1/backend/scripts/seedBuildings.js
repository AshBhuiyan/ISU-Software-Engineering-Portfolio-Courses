import dotenv from 'dotenv';
dotenv.config();

import { connectDB } from '../config/db.js';
import Building from '../models/Building.js';

// default image
const PLACEHOLDER_IMAGE = '/assets/images/Iowa_State_Cyclones_logo.svg.png';

// Mock building data to seed
const mockBuildings = [
  {
    id: 'library',
    name: 'Parks Library',
    code: 'LIB',
    type: 'Academic',
    departments: ['Library Services', 'Research Support', 'Study Spaces'],
    description: 'The main library serving Iowa State University, featuring extensive collections, study spaces, and research support services.',
    hours: 'Mon-Thu: 7:30am-2:00am, Fri: 7:30am-10:00pm',
    capacity: '2,500 students',
    yearBuilt: '1925',
    floors: 4,
    coordinates: { x: 42, y: 12 }, // top: 60px (12%), left: 42%
    image: '/assets/images/parks1.jpeg',
    gallery: ['/assets/images/parks1.jpeg', '/assets/images/parks2.jpg', '/assets/images/parks3.jpeg'],
    video: '/assets/videos/parksvideo.mp4'
  },
  {
    id: 'beardshear',
    name: 'Beardshear Hall',
    code: 'BSH',
    type: 'Administration',
    departments: ['President\'s Office', 'Registrar', 'Admissions'],
    description: 'Historic administration building housing the Office of the President, Registrar, and other central university services.',
    hours: 'Mon-Fri: 8:00am-5:00pm',
    capacity: '200 staff',
    yearBuilt: '1906',
    floors: 3,
    coordinates: { x: 75, y: 8 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'memorial',
    name: 'Memorial Union',
    code: 'MU',
    type: 'Student Life',
    departments: ['Dining Services', 'Student Organizations', 'Bookstore'],
    description: 'The heart of student life featuring dining, meeting spaces, bookstore, and various student services.',
    hours: 'Daily: 6:00am-12:00am',
    capacity: '3,000 visitors daily',
    yearBuilt: '1928',
    floors: 4,
    coordinates: { x: 50, y: 40 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'campanile',
    name: 'Campanile',
    code: 'CAMP',
    type: 'Landmark',
    departments: ['Campus Tours', 'University Relations'],
    description: 'Iowa State\'s iconic 110-foot bell tower, a beloved landmark and symbol of the university since 1899.',
    hours: 'Viewable 24/7, Tours by appointment',
    capacity: 'Landmark viewing',
    yearBuilt: '1899',
    floors: 1,
    coordinates: { x: 15, y: 56 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'hilton',
    name: 'Hilton Coliseum',
    code: 'HIL',
    type: 'Athletics',
    departments: ['Athletics', 'Event Management', 'Ticket Office'],
    description: 'Home of Cyclone athletics, hosting basketball games, concerts, and major university events.',
    hours: 'Event-dependent, Box office: Mon-Fri 9:00am-5:00pm',
    capacity: '14,267 seats',
    yearBuilt: '1971',
    floors: 3,
    coordinates: { x: 85, y: 50 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'carver',
    name: 'Carver Hall',
    code: 'CAR',
    type: 'Academic',
    departments: ['Engineering', 'Computer Science', 'Technology'],
    description: 'Modern academic building housing engineering and technology programs with state-of-the-art laboratories.',
    hours: 'Mon-Fri: 7:00am-10:00pm, Sat-Sun: 8:00am-8:00pm',
    capacity: '1,800 students',
    yearBuilt: '1995',
    floors: 5,
    coordinates: { x: 90, y: 86 }, // bottom: 70px (86%), right: 10% (x: 90%)
    image: '/assets/images/Carverhall.jpg',
    gallery: ['/assets/images/carver2.jpeg', '/assets/images/carver0101.jpg'],
    video: null
  },
  {
    id: 'friley',
    name: 'Friley Hall',
    code: 'FRI',
    type: 'Residence',
    departments: ['Residence Life', 'Dining Services'],
    description: 'Residence hall providing comfortable living spaces for undergraduate students with dining facilities.',
    hours: '24/7 Residential Access',
    capacity: '1,200 residents',
    yearBuilt: '1965',
    floors: 8,
    coordinates: { x: 8, y: 80 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'coover',
    name: 'Coover Hall',
    code: 'COV',
    type: 'Academic',
    departments: ['Electrical Engineering', 'Computer Engineering', 'Cybersecurity'],
    description: 'State-of-the-art engineering facility featuring advanced laboratories and research spaces.',
    hours: 'Mon-Fri: 7:00am-11:00pm, Sat-Sun: 8:00am-10:00pm',
    capacity: '2,200 students',
    yearBuilt: '2010',
    floors: 6,
    coordinates: { x: 45, y: 20 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'sukup',
    name: 'Sukup Hall',
    code: 'SUK',
    type: 'Academic',
    departments: ['Agricultural Engineering', 'Biosystems Engineering'],
    description: 'Modern facility dedicated to agricultural and biosystems engineering research and education.',
    hours: 'Mon-Fri: 7:00am-10:00pm',
    capacity: '1,500 students',
    yearBuilt: '2013',
    floors: 4,
    coordinates: { x: 65, y: 70 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  },
  {
    id: 'state-gym',
    name: 'State Gymnasium',
    code: 'STG',
    type: 'Athletics',
    departments: ['Recreation Services', 'Fitness Programs'],
    description: 'Comprehensive fitness and recreation facility offering various sports and wellness programs.',
    hours: 'Mon-Fri: 5:30am-11:00pm, Sat-Sun: 8:00am-10:00pm',
    capacity: '1,000+ users daily',
    yearBuilt: '1975',
    floors: 3,
    coordinates: { x: 30, y: 76 },
    image: PLACEHOLDER_IMAGE,
    gallery: [PLACEHOLDER_IMAGE],
    video: null
  }
];

async function seedBuildings() {
  try {
    await connectDB();
    
    console.log('🌱 Seeding buildings...');
    
    // Clear existing buildings 
 
    
    let created = 0;
    let skipped = 0;
    
    for (const buildingData of mockBuildings) {
      const existing = await Building.findOne({ id: buildingData.id });
      
      if (existing) {
        console.log(`⏭️  Skipping ${buildingData.name} (already exists)`);
        skipped++;
      } else {
        const building = new Building(buildingData);
        await building.save();
        console.log(`✅ Created ${buildingData.name}`);
        created++;
      }
    }
    
    console.log(`\n✨ Seeding complete!`);
    console.log(`   Created: ${created}`);
    console.log(`   Skipped: ${skipped}`);
    console.log(`   Total: ${mockBuildings.length}`);
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Error seeding buildings:', error);
    process.exit(1);
  }
}

seedBuildings();
