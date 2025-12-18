import express from 'express';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const router = express.Router();


const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// checks if uploads directory exists
const uploadsDir = path.join(__dirname, '../../frontend/public/assets/uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Multer storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    // Generate unique filename: timestamp-originalname
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    const name = path.basename(file.originalname, ext);
    cb(null, `${name}-${uniqueSuffix}${ext}`);
  }
});

// File filter - allow images and videos 
const fileFilter = (req, file, cb) => {
  // Log for debugging
  console.log('File filter check:', {
    fieldname: file.fieldname,
    filename: file.originalname,
    mimetype: file.mimetype,
    extname: path.extname(file.originalname).toLowerCase()
  });

  // Only process files with fieldname 'file'
  if (file.fieldname !== 'file') {
    console.log('Rejecting file - wrong fieldname:', file.fieldname);
    return cb(new Error(`Unexpected field name: "${file.fieldname}". Expected "file".`), false);
  }

  const allowedImageExts = /\.(jpeg|jpg|png|gif|webp)$/i;
  const allowedVideoExts = /\.(mp4|webm|ogg|mov)$/i;
  const extname = path.extname(file.originalname).toLowerCase();
  
  // Check by extension
  const isImageByExt = allowedImageExts.test(extname);
  const isVideoByExt = allowedVideoExts.test(extname);
  
  // accept any video or image
  const isImageMime = /^image\//.test(file.mimetype);
  const isVideoMime = /^video\//.test(file.mimetype);

  // Accept if extension OR mimetype matches (not requiring both)
  if ((isImageByExt || isImageMime) || (isVideoByExt || isVideoMime)) {
    console.log('File accepted');
    cb(null, true);
  } else {
    console.log('File rejected - type not allowed');
    cb(new Error(`File type not allowed. Extension: ${extname}, MIME type: ${file.mimetype}. Only image files (jpeg, jpg, png, gif, webp) and video files (mp4, webm, ogg, mov) are allowed`), false);
  }
};

// Configure Multer
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 50 * 1024 * 1024 // 50MB limit 
  },
  fileFilter: fileFilter
});

/**
 * POST /api/upload
 * Upload a single image or video file
 */
router.post('/', (req, res, next) => {
  upload.single('file')(req, res, (err) => {
    // Handle Multer errors first
    if (err) {
      console.error('Multer error:', {
        code: err.code,
        field: err.field,
        message: err.message,
        name: err.name
      });
      
      if (err instanceof multer.MulterError) {
        if (err.code === 'LIMIT_FILE_SIZE') {
          return res.status(400).json({
            success: false,
            message: 'File too large. Maximum size is 50MB.'
          });
        }
        if (err.code === 'LIMIT_UNEXPECTED_FILE') {
          return res.status(400).json({
            success: false,
            message: `Unexpected field: "${err.field}". Please use the field name "file" when uploading.`
          });
        }
        return res.status(400).json({
          success: false,
          message: err.message || 'File upload error',
          error: err.code || 'UPLOAD_ERROR'
        });
      }
      
      // Handle file filter errors
      return res.status(400).json({
        success: false,
        message: err.message || 'File upload error'
      });
    }

    // If no error, process the file
    try {
      // Log request details for debugging
      console.log('Upload request received:', {
        contentType: req.headers['content-type'],
        hasFile: !!req.file,
        fileField: req.file ? req.file.fieldname : 'none'
      });

      if (!req.file) {
        return res.status(400).json({
          success: false,
          message: 'No file uploaded. Please select a file.'
        });
      }

      // Determine file type
      const ext = path.extname(req.file.originalname).toLowerCase();
      const isVideo = /mp4|webm|ogg|mov/.test(ext);
      const fileType = isVideo ? 'video' : 'image';

      // Return the public URL
      const publicUrl = `/assets/uploads/${req.file.filename}`;

      console.log('File uploaded successfully:', {
        filename: req.file.filename,
        originalname: req.file.originalname,
        mimetype: req.file.mimetype,
        size: req.file.size,
        type: fileType
      });

      res.json({
        success: true,
        url: publicUrl,
        filename: req.file.filename,
        type: fileType
      });
    } catch (error) {
      console.error('Error processing uploaded file:', error);
      res.status(500).json({
        success: false,
        message: 'Failed to process uploaded file',
        error: error.message
      });
    }
  });
});

export default router;
