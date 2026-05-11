const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const dashboardController = require('../controllers/dashboardController');

router.get('/', authMiddleware, dashboardController.getDashboard);

module.exports = router;