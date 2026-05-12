const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const adminController = require('../controllers/adminController');

router.get('/users', authMiddleware, adminController.listarUsuarios);

module.exports = router;