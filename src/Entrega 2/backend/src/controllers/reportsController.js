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

function mapTrend(status) {
  if (status === 'em_alta') return 'UP';
  if (status === 'em_queda') return 'DOWN';
  return 'STABLE';
}

exports.operatorAttributes = (req, res) => {
  const sql = `
    SELECT 
      u.id_usuario,
      u.nome,
      u.username,
      r.nome AS role,
      COALESCE(p.total_produtos, 0) AS total_produtos,
      COALESCE(p.precisao, 0) AS precisao,
      COALESCE(p.streak, 0) AS streak,
      COALESCE(p.status_tendencia, 'estavel') AS status_tendencia
    FROM usuarios u
    JOIN roles r ON u.id_role = r.id_role
    LEFT JOIN performance_usuario p ON u.id_usuario = p.id_usuario
    WHERE r.nome = 'Operador'
    ORDER BY u.nome
  `;

  db.query(sql, (err, operadores) => {
    if (err) return res.status(500).json(err);

    const members = operadores.map((op) => ({
      id: `mbr_${String(op.id_usuario).padStart(3, '0')}`,
      name: op.nome,
      role: normalizarRole(op.role),
      joinDate: '2026-01-10',
      trend: mapTrend(op.status_tendencia),
      currentScore: Number(op.precisao),
      previousScore: Math.max(Number(op.precisao) - 4, 0),
      scoreDelta: 4,
      leaderNote: 'Registro gerado com base no desempenho operacional do usuário.',
      actionPlan: 'Acompanhar evolução semanal e reforçar boas práticas de contagem.',
      attributes: [
        {
          name: 'Precisao',
          score: Number(op.precisao),
          previousScore: Math.max(Number(op.precisao) - 4, 0),
          color: '#00E676'
        },
        {
          name: 'Agilidade',
          score: Math.min(Number(op.total_produtos) * 5, 100),
          previousScore: Math.max(Math.min(Number(op.total_produtos) * 5, 100) - 5, 0),
          color: '#1A73E8'
        },
        {
          name: 'Organizacao',
          score: Math.min(Number(op.streak) * 10, 100),
          previousScore: Math.max(Math.min(Number(op.streak) * 10, 100) - 3, 0),
          color: '#00BCD4'
        },
        {
          name: 'Autonomia',
          score: 78,
          previousScore: 72,
          color: '#7C4DFF'
        }
      ],
      weeklyEvolution: {
        labels: ['S-6', 'S-5', 'S-4', 'S-3', 'S-2', 'S-1'],
        values: [72, 76, 78, 81, 84, Number(op.precisao) || 86]
      },
      monthlyEvolution: {
        labels: ['Nov', 'Dez', 'Jan', 'Fev', 'Mar', 'Abr'],
        values: [68, 71, 74, 79, 83, Number(op.precisao) || 86]
      }
    }));

    const totalScores = members.reduce((sum, m) => sum + m.currentScore, 0);
    const teamAttributeAverage = members.length
      ? Math.round(totalScores / members.length)
      : 0;

    res.json({
      summary: {
        teamAttributeAverage,
        averageDelta: 4,
        attentionCount: members.filter(m => m.trend === 'DOWN').length,
        bestEvolutionMemberId: members[0]?.id || null
      },
      attributeMap: [
        {
          name: 'Precisao',
          score: teamAttributeAverage,
          previousScore: Math.max(teamAttributeAverage - 4, 0),
          color: '#00E676'
        },
        {
          name: 'Agilidade',
          score: 78,
          previousScore: 74,
          color: '#1A73E8'
        },
        {
          name: 'Organizacao',
          score: 79,
          previousScore: 77,
          color: '#00BCD4'
        }
      ],
      trendCounts: {
        evolving: members.filter(m => m.trend === 'UP').length,
        maintaining: members.filter(m => m.trend === 'STABLE').length,
        attention: members.filter(m => m.trend === 'DOWN').length
      },
      members
    });
  });
};