const db = require('../config/database');

exports.getDashboard = (req, res) => {
  const idEquipe = req.usuario.id_equipe;

  const sqlResumo = `
    SELECT 
      COUNT(*) AS recognizedProducts,
      0 AS unrecognizedProducts,
      COUNT(*) AS teamTotalProducts
    FROM registros
    WHERE id_equipe = ?
  `;

  const sqlPorDia = `
    SELECT 
      DAYOFWEEK(data_registro) AS dia_semana,
      COUNT(*) AS total
    FROM registros
    WHERE id_equipe = ?
    GROUP BY DAYOFWEEK(data_registro)
  `;

  db.query(sqlResumo, [idEquipe], (err, resumoResult) => {
    if (err) return res.status(500).json(err);

    db.query(sqlPorDia, [idEquipe], (err, diasResult) => {
      if (err) return res.status(500).json(err);

      const labels = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab'];
      const values = [0, 0, 0, 0, 0, 0, 0];

      diasResult.forEach((item) => {
        const index = item.dia_semana - 1;
        values[index] = item.total;
      });

      let acumulado = 0;
      const accumulatedValues = values.map((valor) => {
        acumulado += valor;
        return acumulado;
      });

      const recognizedProducts = resumoResult[0].recognizedProducts || 0;
      const unrecognizedProducts = resumoResult[0].unrecognizedProducts || 0;
      const teamTotalProducts = resumoResult[0].teamTotalProducts || 0;

      const target = 600;
      const percentage = target > 0
        ? Math.round((recognizedProducts / target) * 100)
        : 0;

      res.json({
        title: 'Dashboard',
        subtitle: 'Visao geral da equipe',
        summary: {
          recognizedProducts,
          unrecognizedProducts,
          teamTotalProducts
        },
        readingDistribution: {
          recognized: recognizedProducts,
          unrecognized: unrecognizedProducts,
          totalRead: teamTotalProducts
        },
        productsByDay: {
          labels,
          values
        },
        accumulatedEvolution: {
          labels,
          values: accumulatedValues
        },
        teamGoal: {
          current: recognizedProducts,
          target,
          percentage
        }
      });
    });
  });
};