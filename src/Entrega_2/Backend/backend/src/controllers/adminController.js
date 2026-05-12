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
exports.listarUsuarios = (req, res) => {
  const currentUserId = req.usuario.id;

  const sql = `
    SELECT 
      u.id_usuario,
      u.nome,
      u.username,
      r.nome AS role
    FROM usuarios u
    JOIN roles r ON u.id_role = r.id_role
    ORDER BY r.nome, u.nome
  `;

  db.query(sql, (err, usuarios) => {
    if (err) return res.status(500).json(err);

    const users = usuarios.map((u) => ({
      id: `usr_${String(u.id_usuario).padStart(3, '0')}`,
      name: u.nome,
      username: u.username,
      role: normalizarRole(u.role),
      isSelf: u.id_usuario === currentUserId
    }));

    const countByRole = {
      OPERADOR: 0,
      SUPERVISAO: 0,
      CONSELHO_MENTORIA: 0,
      COORDENACAO: 0,
      ADMINISTRADOR: 0
    };

    users.forEach((u) => {
      if (countByRole[u.role] !== undefined) {
        countByRole[u.role]++;
      }
    });

    const currentUser = users.find((u) => u.isSelf);

    res.json({
      currentUser,
      stats: {
        totalUsers: users.length,
        countByRole
      },
      availableRoles: [
        'OPERADOR',
        'SUPERVISAO',
        'CONSELHO_MENTORIA',
        'COORDENACAO',
        'ADMINISTRADOR'
      ],
      creatableRoles: [
        'SUPERVISAO',
        'CONSELHO_MENTORIA',
        'COORDENACAO',
        'ADMINISTRADOR'
      ],
      users
    });
  });
};