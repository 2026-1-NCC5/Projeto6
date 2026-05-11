const db = require('../config/database');

exports.registrarScan = (req, res) => {
  const { sku } = req.body;

  if (!sku) {
    return res.status(400).json({
      message: 'SKU é obrigatório'
    });
  }

  const sqlProduto = `
    SELECT *
    FROM produtos
    WHERE sku = ?
  `;

  db.query(sqlProduto, [sku], (err, produtos) => {
    if (err) return res.status(500).json(err);

    if (produtos.length === 0) {
      return res.status(404).json({
        message: 'Produto não encontrado'
      });
    }

    const produto = produtos[0];

    const updateSql = `
      UPDATE produtos
      SET quantidade = quantidade + 1,
          atualizado_em = NOW()
      WHERE id_produto = ?
    `;

    db.query(updateSql, [produto.id_produto], (err) => {
      if (err) return res.status(500).json(err);

      const insertRegistro = `
        INSERT INTO registros
        (id_usuario, id_equipe, id_produto, quantidade)
        VALUES (?, ?, ?, ?)
      `;

      db.query(
        insertRegistro,
        [
          req.usuario.id,
          req.usuario.id_equipe,
          produto.id_produto,
          1
        ],
        (err, result) => {
          if (err) return res.status(500).json(err);

          res.status(201).json({
            scan: {
              id: `scan_${result.insertId}`,
              sku: produto.sku,
              productName: produto.nome,
              quantityAdded: 1,
              detectedAt: new Date(),
              operator: {
                id: req.usuario.id,
                username: req.usuario.username
              }
            },
            inventoryUpdate: {
              previousQuantity: produto.quantidade,
              newQuantity: produto.quantidade + 1
            }
          });
        }
      );
    });
  });
};