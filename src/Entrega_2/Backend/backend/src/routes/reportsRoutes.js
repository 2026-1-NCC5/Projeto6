const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const reportsController = require('../controllers/reportsController');

router.get('/operator-attributes', authMiddleware, reportsController.operatorAttributes);

module.exports = router;