const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const cameraController = require('../controllers/cameraController');

router.post('/scans', authMiddleware, cameraController.registrarScan);

module.exports = router;