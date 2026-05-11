const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const meController = require('../controllers/meController');

router.get('/settings', authMiddleware, meController.getSettings);

module.exports = router;