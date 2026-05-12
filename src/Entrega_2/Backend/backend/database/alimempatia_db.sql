-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: alimempatia_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `categorias`
--

DROP TABLE IF EXISTS `categorias`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorias` (
  `id_categoria` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `nome` (`nome`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categorias`
--

LOCK TABLES `categorias` WRITE;
/*!40000 ALTER TABLE `categorias` DISABLE KEYS */;
INSERT INTO `categorias` VALUES (1,'Cereais'),(5,'Lacticinios'),(2,'Leguminosas'),(3,'Massas'),(4,'Oleos'),(6,'Outros');
/*!40000 ALTER TABLE `categorias` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deteccoes`
--

DROP TABLE IF EXISTS `deteccoes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deteccoes` (
  `id_deteccao` int NOT NULL AUTO_INCREMENT,
  `id_sessao` int NOT NULL,
  `id_produto` int DEFAULT NULL,
  `classe_detectada` enum('arroz','feijao','macarrao','oleo','leite','outros') NOT NULL,
  `confianca` decimal(5,2) DEFAULT NULL,
  `reconhecido` tinyint(1) DEFAULT '1',
  `imagem_url` text,
  `detectado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_deteccao`),
  KEY `id_sessao` (`id_sessao`),
  KEY `id_produto` (`id_produto`),
  CONSTRAINT `deteccoes_ibfk_1` FOREIGN KEY (`id_sessao`) REFERENCES `sessoes` (`id_sessao`),
  CONSTRAINT `deteccoes_ibfk_2` FOREIGN KEY (`id_produto`) REFERENCES `produtos` (`id_produto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deteccoes`
--

LOCK TABLES `deteccoes` WRITE;
/*!40000 ALTER TABLE `deteccoes` DISABLE KEYS */;
/*!40000 ALTER TABLE `deteccoes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `equipes`
--

DROP TABLE IF EXISTS `equipes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipes` (
  `id_equipe` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_equipe`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `equipes`
--

LOCK TABLES `equipes` WRITE;
/*!40000 ALTER TABLE `equipes` DISABLE KEYS */;
INSERT INTO `equipes` VALUES (1,'Equipe Principal','2026-04-26 13:48:25'),(2,'Equipe Alpha','2026-04-28 12:45:21'),(3,'Equipe Bravo','2026-04-28 12:45:21'),(4,'Equipe Charlie','2026-04-28 12:45:21');
/*!40000 ALTER TABLE `equipes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `historico_semanal`
--

DROP TABLE IF EXISTS `historico_semanal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historico_semanal` (
  `id_historico` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `semana` varchar(20) NOT NULL,
  `total_produtos` int DEFAULT '0',
  PRIMARY KEY (`id_historico`),
  KEY `id_usuario` (`id_usuario`),
  CONSTRAINT `historico_semanal_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `historico_semanal`
--

LOCK TABLES `historico_semanal` WRITE;
/*!40000 ALTER TABLE `historico_semanal` DISABLE KEYS */;
/*!40000 ALTER TABLE `historico_semanal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `performance_usuario`
--

DROP TABLE IF EXISTS `performance_usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `performance_usuario` (
  `id_performance` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `total_produtos` int DEFAULT '0',
  `precisao` decimal(5,2) DEFAULT '0.00',
  `streak` int DEFAULT '0',
  `status_tendencia` enum('em_alta','estavel','em_queda') DEFAULT 'estavel',
  `atualizado_em` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_performance`),
  KEY `id_usuario` (`id_usuario`),
  CONSTRAINT `performance_usuario_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `performance_usuario`
--

LOCK TABLES `performance_usuario` WRITE;
/*!40000 ALTER TABLE `performance_usuario` DISABLE KEYS */;
/*!40000 ALTER TABLE `performance_usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `produtos`
--

DROP TABLE IF EXISTS `produtos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `produtos` (
  `id_produto` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `sku` varchar(50) NOT NULL,
  `id_categoria` int DEFAULT NULL,
  `quantidade` int DEFAULT '0',
  `atualizado_em` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `peso_kg` decimal(6,2) DEFAULT '0.00',
  PRIMARY KEY (`id_produto`),
  UNIQUE KEY `sku` (`sku`),
  KEY `id_categoria` (`id_categoria`),
  CONSTRAINT `produtos_ibfk_1` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `produtos`
--

LOCK TABLES `produtos` WRITE;
/*!40000 ALTER TABLE `produtos` DISABLE KEYS */;
INSERT INTO `produtos` VALUES (1,'Arroz 5kg','ARR-005',1,10,'2026-04-28 15:00:35',5.00),(2,'Feijão','SKU-LE-002',2,2,'2026-04-26 13:50:33',0.00),(3,'Macarrão','SKU-MA-003',3,0,'2026-04-26 13:50:33',0.00),(4,'Óleo','SKU-OL-004',4,9,'2026-04-26 13:50:33',0.00),(5,'Leite','SKU-LA-005',5,9,'2026-04-26 13:50:33',0.00),(6,'Arroz 1kg','ARR-001',1,0,'2026-04-28 15:01:07',1.00),(7,'Arroz 10kg','ARR-010',1,0,'2026-04-28 15:01:07',10.00),(8,'Feijão 1kg','FEI-001',2,0,'2026-04-28 15:01:37',1.00),(9,'Feijão 5kg','FEI-005',2,0,'2026-04-28 15:01:37',5.00),(10,'Macarrão 500g','MAC-500',3,0,'2026-04-28 15:01:37',0.50),(11,'Óleo 900ml','OLE-900',4,0,'2026-04-28 15:01:37',0.90),(12,'Leite 1L','LEI-001',5,0,'2026-04-28 15:01:37',1.00);
/*!40000 ALTER TABLE `produtos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrcodes`
--

DROP TABLE IF EXISTS `qrcodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrcodes` (
  `id_qrcode` int NOT NULL AUTO_INCREMENT,
  `id_sessao` int NOT NULL,
  `codigo` text NOT NULL,
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_qrcode`),
  KEY `id_sessao` (`id_sessao`),
  CONSTRAINT `qrcodes_ibfk_1` FOREIGN KEY (`id_sessao`) REFERENCES `sessoes` (`id_sessao`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrcodes`
--

LOCK TABLES `qrcodes` WRITE;
/*!40000 ALTER TABLE `qrcodes` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrcodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registros`
--

DROP TABLE IF EXISTS `registros`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registros` (
  `id_registro` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `id_equipe` int NOT NULL,
  `id_produto` int NOT NULL,
  `quantidade` int DEFAULT '1',
  `data_registro` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_registro`),
  KEY `id_usuario` (`id_usuario`),
  KEY `id_equipe` (`id_equipe`),
  KEY `id_produto` (`id_produto`),
  CONSTRAINT `registros_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `registros_ibfk_2` FOREIGN KEY (`id_equipe`) REFERENCES `equipes` (`id_equipe`),
  CONSTRAINT `registros_ibfk_3` FOREIGN KEY (`id_produto`) REFERENCES `produtos` (`id_produto`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registros`
--

LOCK TABLES `registros` WRITE;
/*!40000 ALTER TABLE `registros` DISABLE KEYS */;
INSERT INTO `registros` VALUES (1,11,2,1,1,'2026-04-28 12:57:30'),(2,11,2,1,1,'2026-04-28 16:44:52');
/*!40000 ALTER TABLE `registros` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id_role` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(50) NOT NULL,
  PRIMARY KEY (`id_role`),
  UNIQUE KEY `nome` (`nome`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (5,'Administrador'),(3,'Conselho de Mentoria'),(4,'Coordenação'),(1,'Operador'),(2,'Supervisão');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sessoes`
--

DROP TABLE IF EXISTS `sessoes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sessoes` (
  `id_sessao` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `id_equipe` int NOT NULL,
  `data_inicio` datetime DEFAULT CURRENT_TIMESTAMP,
  `data_fim` datetime DEFAULT NULL,
  `status` enum('ativa','finalizada') DEFAULT 'ativa',
  PRIMARY KEY (`id_sessao`),
  KEY `id_usuario` (`id_usuario`),
  KEY `id_equipe` (`id_equipe`),
  CONSTRAINT `sessoes_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `sessoes_ibfk_2` FOREIGN KEY (`id_equipe`) REFERENCES `equipes` (`id_equipe`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sessoes`
--

LOCK TABLES `sessoes` WRITE;
/*!40000 ALTER TABLE `sessoes` DISABLE KEYS */;
/*!40000 ALTER TABLE `sessoes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `username` varchar(100) NOT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `id_role` int NOT NULL,
  `id_equipe` int DEFAULT NULL,
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `username` (`username`),
  KEY `id_role` (`id_role`),
  KEY `id_equipe` (`id_equipe`),
  CONSTRAINT `usuarios_ibfk_1` FOREIGN KEY (`id_role`) REFERENCES `roles` (`id_role`),
  CONSTRAINT `usuarios_ibfk_2` FOREIGN KEY (`id_equipe`) REFERENCES `equipes` (`id_equipe`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'Admin Sistema','admin','admin123',5,1,'2026-04-26 13:52:59'),(2,'Operador Teste','operador','operador123',1,1,'2026-04-26 13:52:59'),(3,'Supervisao Teste','supervisao','super123',2,1,'2026-04-26 13:52:59'),(4,'Conselho Teste','conselho','conselho123',3,1,'2026-04-26 13:52:59'),(5,'Coordenacao Teste','coordenacao','coord123',4,1,'2026-04-26 13:52:59'),(6,'Lucas Andrade','lucas_op','123',1,1,'2026-04-28 12:46:03'),(7,'Mariana Souza','mariana_sup','123',2,1,'2026-04-28 12:46:03'),(8,'Ricardo Lima','ricardo_con','123',3,1,'2026-04-28 12:46:03'),(9,'Fernanda Alves','fernanda_coor','123',4,1,'2026-04-28 12:46:03'),(10,'Bruno Martins','bruno_admin','123',5,1,'2026-04-28 12:46:03'),(11,'Pedro Henrique','pedro_op','123',1,2,'2026-04-28 12:46:24'),(12,'Camila Rocha','camila_sup','123',2,2,'2026-04-28 12:46:24'),(13,'Eduardo Silva','eduardo_con','123',3,2,'2026-04-28 12:46:24'),(14,'Juliana Costa','juliana_coor','123',4,2,'2026-04-28 12:46:24'),(15,'Rafael Gomes','rafael_admin','123',5,2,'2026-04-28 12:46:24'),(16,'Thiago Fernandes','thiago_op','123',1,3,'2026-04-28 12:46:48'),(17,'Aline Barros','aline_sup','123',2,3,'2026-04-28 12:46:48'),(18,'Gustavo Nunes','gustavo_con','123',3,3,'2026-04-28 12:46:48'),(19,'Patricia Duarte','patricia_coor','123',4,3,'2026-04-28 12:46:48'),(20,'Daniel Ribeiro','daniel_admin','123',5,3,'2026-04-28 12:46:48');
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'alimempatia_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-06 10:02:10
