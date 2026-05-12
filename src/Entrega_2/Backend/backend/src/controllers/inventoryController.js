const db = require('../config/database');

function calcularStatus(quantity, minStock) {
  if (quantity <= 0) return 'OUT_OF_STOCK';
  if (quantity <= minStock) return 'LOW_STOCK';
  return 'IN_STOCK';
}

function formatarProduto(p) {
  const minStock = 10;
  const status = calcularStatus(p.quantidade, minStock);

  return {
    id: `prd_${String(p.id_produto).padStart(3, '0')}`,
    name: p.nome,
    sku: p.sku,
    category: p.categoria ? p.categoria.toUpperCase() : 'OUTROS',
    quantity: p.quantidade,
    minStock,
    status,
    imageKey: p.nome.toLowerCase().split(' ')[0],
    detectedAt: p.atualizado_em
  };
}

exports.listarInventario = (req, res) => {
  const sql = `
    SELECT 
      p.id_produto,
      p.nome,
      p.sku,
      p.quantidade,
      p.peso_kg,
      p.atualizado_em,
      c.nome AS categoria
    FROM produtos p
    LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
    ORDER BY p.nome ASC
  `;

  db.query(sql, (err, produtos) => {
    if (err) return res.status(500).json(err);

    const products = produtos.map(formatarProduto);

    res.json({
      cameraStatus: {
        id: 'cam_001',
        name: 'Camera 01',
        location: 'Esteira Principal',
        status: 'DETECTING'
      },
      stats: {
        total: products.length,
        inStock: products.filter(p => p.status === 'IN_STOCK').length,
        lowStock: products.filter(p => p.status === 'LOW_STOCK').length,
        outOfStock: products.filter(p => p.status === 'OUT_OF_STOCK').length
      },
      availableCategories: [
        'CEREAIS',
        'LEGUMINOSAS',
        'MASSAS',
        'OLEOS',
        'LATICINIOS',
        'OUTROS'
      ],
      availableStatuses: [
        'IN_STOCK',
        'LOW_STOCK',
        'OUT_OF_STOCK'
      ],
      products
    });
  });
};

exports.detalharProduto = (req, res) => {
  const { productId } = req.params;

  const idProduto = productId.replace('prd_', '');

  const sql = `
    SELECT 
      p.id_produto,
      p.nome,
      p.sku,
      p.quantidade,
      p.peso_kg,
      p.atualizado_em,
      c.nome AS categoria
    FROM produtos p
    LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
    WHERE p.id_produto = ?
  `;

  db.query(sql, [idProduto], (err, result) => {
    if (err) return res.status(500).json(err);

    if (result.length === 0) {
      return res.status(404).json({ message: 'Produto não encontrado' });
    }

    const p = result[0];
    const product = formatarProduto(p);

    res.json({
      product,
      stockLevel: {
        current: product.quantity,
        minimum: product.minStock,
        progress: product.minStock > 0
          ? Number((product.quantity / product.minStock).toFixed(2))
          : 0
      },
      aiDetection: {
        cameraId: 'cam_001',
        cameraName: 'Camera 01',
        location: 'Esteira Principal',
        confidence: 98.4,
        detectedAt: product.detectedAt
      }
    });
  });
};