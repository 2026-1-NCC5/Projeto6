const db = require('../config/database');

exports.criarRegistro = (req, res) => {
  const { sku } = req.body;

  const id_usuario = req.usuario.id;
  const id_equipe = req.usuario.id_equipe;

  if (!sku) {
    return res.status(400).json({ message: 'SKU é obrigatório' });
  }

  const sqlProduto = `
    SELECT id_produto, nome, sku
    FROM produtos
    WHERE sku = ?
  `;

  db.query(sqlProduto, [sku], (err, produtos) => {
    if (err) return res.status(500).json(err);

    if (produtos.length === 0) {
      return res.status(404).json({ message: 'Produto não encontrado' });
    }

    const produto = produtos[0];

    const sqlRegistro = `
      INSERT INTO registros (id_usuario, id_equipe, id_produto)
      VALUES (?, ?, ?)
    `;

    db.query(sqlRegistro, [id_usuario, id_equipe, produto.id_produto], (err, result) => {
      if (err) return res.status(500).json(err);

      res.status(201).json({
        message: 'Registro criado com sucesso',
        registro: {
          id_registro: result.insertId,
          id_usuario,
          id_equipe,
          produto: produto.nome,
          sku: produto.sku,
          quantidade: 1
        }
      });
    });
  });
};