const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddleware');
const inventoryController = require('../controllers/inventoryController');

router.get('/', authMiddleware, inventoryController.listarInventario);

router.get(
  '/:productId',
  authMiddleware,
  inventoryController.detalharProduto
);

module.exports = router;