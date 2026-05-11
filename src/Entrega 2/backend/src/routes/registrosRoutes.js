const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const registrosController = require('../controllers/registrosController');

router.post('/', authMiddleware, registrosController.criarRegistro);

module.exports = router;