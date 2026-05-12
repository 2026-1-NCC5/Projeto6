const db = require('../config/database');
const jwt = require('jsonwebtoken');

exports.login = (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({
      message: 'Username e password são obrigatórios'
    });
  }

  const sql = `
    SELECT u.*, r.nome AS role
    FROM usuarios u
    JOIN roles r ON u.id_role = r.id_role
    WHERE u.username = ?
  `;

  db.query(sql, [username], (err, result) => {
    if (err) return res.status(500).json(err);

    if (result.length === 0) {
      return res.status(401).json({ message: 'Usuário não encontrado' });
    }

    const user = result[0];

    if (password !== user.senha_hash) {
      return res.status(401).json({ message: 'Senha incorreta' });
    }

    const token = jwt.sign(
      {
        id: user.id_usuario,
        username: user.username,
        role: user.role,
        id_equipe: user.id_equipe
      },
      process.env.JWT_SECRET,
      { expiresIn: '8h' }
    );

    return res.json({
      token,
      user: {
        id: user.id_usuario,
        name: user.nome,
        username: user.username,
        role: user.role,
        id_equipe: user.id_equipe
      }
    });
  });
};

exports.register = (req, res) => {
  const { name, username, password, confirmPassword } = req.body;

  if (!name || !username || !password || !confirmPassword) {
    return res.status(400).json({
      message: 'Todos os campos são obrigatórios'
    });
  }

  if (password !== confirmPassword) {
    return res.status(400).json({
      message: 'As senhas não coincidem'
    });
  }

  const verificarSql = `
    SELECT * FROM usuarios
    WHERE username = ?
  `;

  db.query(verificarSql, [username], (err, result) => {
    if (err) return res.status(500).json(err);

    if (result.length > 0) {
      return res.status(409).json({
        message: 'Username já existe'
      });
    }

    const insertSql = `
      INSERT INTO usuarios
      (nome, username, senha_hash, id_role, id_equipe)
      VALUES (?, ?, ?, ?, ?)
    `;

    const valores = [
      name,
      username,
      password,
      1,
      1
    ];

    db.query(insertSql, valores, (err, insertResult) => {
      if (err) return res.status(500).json(err);

      res.status(201).json({
        user: {
          id: `usr_${String(insertResult.insertId).padStart(3, '0')}`,
          name,
          username,
          role: 'OPERADOR'
        }
      });
    });
  });
};