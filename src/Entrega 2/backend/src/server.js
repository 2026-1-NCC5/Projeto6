require('dotenv').config();
const express = require('express');
const cors = require('cors');

const app = express();

app.use(cors());
app.use(express.json());

const authRoutes = require('./routes/authRoutes');
const inventoryRoutes = require('./routes/inventoryRoutes');
const authMiddleware = require('./middlewares/authMiddleware');
const dashboardRoutes = require('./routes/dashboardRoutes');
const adminRoutes = require('./routes/adminRoutes');
const meRoutes = require('./routes/meRoutes');
const cameraRoutes = require('./routes/cameraRoutes');
const reportsRoutes = require('./routes/reportsRoutes');

app.use('/api/auth', authRoutes);
app.use('/api/inventory', inventoryRoutes);
app.use('/api/dashboard', dashboardRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/me', meRoutes);
app.use('/api/camera', cameraRoutes);
app.use('/api/reports', reportsRoutes);

app.get('/api/perfil', authMiddleware, (req, res) => {
  res.json({
    message: 'Usuário autenticado',
    usuario: req.usuario
  });
});

app.get('/api/teste', (req, res) => {
  res.json({ message: 'API funcionando' });
});

app.listen(process.env.PORT, () => {
  console.log('Servidor rodando na porta ' + process.env.PORT);
});