-- ============================================================
-- Migração: Adicionar 'acucar' e 'fuba' ao ENUM da tabela deteccoes
-- Execute este script no banco alimempatia_db antes de iniciar
-- o microserviço servico_ia.py
-- ============================================================

USE alimempatia_db;

ALTER TABLE deteccoes
  MODIFY COLUMN classe_detectada
    ENUM('arroz','feijao','macarrao','oleo','leite','acucar','fuba','outros')
    NOT NULL;
