const db = require('../config/database');

function normalizarRole(role) {
  const normalizado = role
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, '_')
    .toUpperCase();

  if (normalizado === 'CONSELHO_DE_MENTORIA') {
    return 'CONSELHO_MENTORIA';
  }

  return normalizado;
}

exports.getSettings = (req, res) => {
  const idUsuario = req.usuario.id;

  const sql = `
    SELECT 
      u.id_usuario,
      u.nome,
      u.username,
      r.nome AS role
    FROM usuarios u
    JOIN roles r ON u.id_role = r.id_role
    WHERE u.id_usuario = ?
  `;

  db.query(sql, [idUsuario], (err, result) => {
    if (err) return res.status(500).json(err);

    if (result.length === 0) {
      return res.status(404).json({ message: 'Usuário não encontrado' });
    }

    const user = result[0];
    const role = normalizarRole(user.role);

    res.json({
      user: {
        id: `usr_${String(user.id_usuario).padStart(3, '0')}`,
        name: user.nome,
        username: user.username,
        role
      },
      rolesHierarchy: [
        'OPERADOR',
        'SUPERVISAO',
        'CONSELHO_MENTORIA',
        'COORDENACAO',
        'ADMINISTRADOR'
      ],
      permissions: {
        canAccessAdminPanel: role === 'ADMINISTRADOR',
        canEditOwnName: true,
        canChangePassword: true
      }
    });
  });
};