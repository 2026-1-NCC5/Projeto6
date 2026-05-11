package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object SessionDatabaseMySQL {

    fun createSession(request: SessionRequest): SessionResponse {
        val connection = DatabaseFactory.getConnection()

        val checkSql = """
            SELECT id_sessao FROM sessoes 
            WHERE id_usuario = ? AND status = 'ativa'
        """.trimIndent()

        try {
            val checkStmt = connection.prepareStatement(checkSql)
            checkStmt.setInt(1, request.userId.toInt())
            val rs = checkStmt.executeQuery()
            if (rs.next()) {
                connection.close()
                return SessionResponse(
                    success = false,
                    message = "Usuário já possui uma sessão ativa"
                )
            }
            rs.close()
            checkStmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val machine = CameraDatabaseMySQL.getMachineByQRCode(request.qrCode)
        if (machine == null) {
            connection.close()
            return SessionResponse(
                success = false,
                message = "QR Code inválido. Máquina não encontrada."
            )
        }

        val equipeId = getOrCreateEquipeId(machine.name)
        val sessionId = UUID.randomUUID().toString()

        val insertSql = """
            INSERT INTO sessoes (id_usuario, id_equipe, data_inicio, status)
            VALUES (?, ?, NOW(), 'ativa')
        """.trimIndent()

        try {
            val stmt = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)
            stmt.setInt(1, request.userId.toInt())
            stmt.setInt(2, equipeId)
            stmt.executeUpdate()

            val generatedKeys = stmt.generatedKeys
            var dbSessionId = 0
            if (generatedKeys.next()) {
                dbSessionId = generatedKeys.getInt(1)
            }
            generatedKeys.close()
            stmt.close()

            val scanSql = """
                INSERT INTO deteccoes (id_sessao, classe_detectada, reconhecido, detectado_em)
                VALUES (?, ?, 1, NOW())
            """.trimIndent()
            val scanStmt = connection.prepareStatement(scanSql)
            scanStmt.setInt(1, dbSessionId)
            scanStmt.setString(2, "qr_scan")
            scanStmt.executeUpdate()
            scanStmt.close()

            connection.close()

            val sessionData = SessionData(
                sessionId = sessionId,
                qrCode = request.qrCode,
                userId = request.userId,
                userName = request.userName,
                sessionType = request.sessionType,
                startTime = request.startTime,
                status = SessionStatus.ACTIVE,
                machineInfo = machine
            )

            return SessionResponse(
                success = true,
                data = sessionData,
                message = "Sessão iniciada com sucesso"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            return SessionResponse(
                success = false,
                message = "Erro ao criar sessão: ${e.message}"
            )
        }
    }

    private fun getOrCreateEquipeId(machineName: String): Int {
        val connection = DatabaseFactory.getConnection()

        val findSql = "SELECT id_equipe FROM equipes WHERE nome = ?"
        try {
            val stmt = connection.prepareStatement(findSql)
            stmt.setString(1, machineName)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val id = rs.getInt("id_equipe")
                rs.close()
                stmt.close()
                connection.close()
                return id
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val insertSql = "INSERT INTO equipes (nome) VALUES (?)"
        try {
            val stmt = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)
            stmt.setString(1, machineName)
            stmt.executeUpdate()
            val rs = stmt.generatedKeys
            if (rs.next()) {
                val newId = rs.getInt(1)
                rs.close()
                stmt.close()
                connection.close()
                return newId
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        connection.close()
        return 1
    }

    fun getActiveSession(userId: String): SessionResponse {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT s.id_sessao, s.data_inicio, e.nome as machine_name
            FROM sessoes s
            LEFT JOIN equipes e ON s.id_equipe = e.id_equipe
            WHERE s.id_usuario = ? AND s.status = 'ativa'
            ORDER BY s.data_inicio DESC LIMIT 1
        """.trimIndent()

        try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, userId.toInt())
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val sessionId = rs.getInt("id_sessao").toString()
                val startTime = rs.getString("data_inicio")
                val machineName = rs.getString("machine_name") ?: "Máquina"

                val startInstant = Instant.parse(startTime)
                val now = Instant.now()
                val minutesElapsed = ChronoUnit.MINUTES.between(startInstant, now)

                if (minutesElapsed >= 60) {
                    endSessionById(sessionId)
                    rs.close()
                    stmt.close()
                    connection.close()
                    return SessionResponse(
                        success = false,
                        message = "Sessão expirada"
                    )
                }

                val sessionData = SessionData(
                    sessionId = sessionId,
                    qrCode = "",
                    userId = userId,
                    userName = "",
                    sessionType = SessionType.WORK,
                    startTime = startTime,
                    status = SessionStatus.ACTIVE,
                    machineInfo = MachineInfo(
                        id = "mac_001",
                        name = machineName,
                        code = machineName,
                        location = "Área de Produção",
                        status = "ONLINE"
                    )
                )
                rs.close()
                stmt.close()
                connection.close()
                return SessionResponse(
                    success = true,
                    data = sessionData,
                    message = "Sessão ativa encontrada"
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        connection.close()
        return SessionResponse(
            success = false,
            message = "Nenhuma sessão ativa encontrada"
        )
    }

    fun endSessionById(sessionId: String): SessionEndResponse {
        val connection = DatabaseFactory.getConnection()
        val sessionDbId = sessionId.toIntOrNull() ?: return SessionEndResponse(
            success = false,
            message = "ID de sessão inválido"
        )

        val getSql = "SELECT data_inicio FROM sessoes WHERE id_sessao = ?"
        var startTime: String? = null

        try {
            val getStmt = connection.prepareStatement(getSql)
            getStmt.setInt(1, sessionDbId)
            val rs = getStmt.executeQuery()
            if (rs.next()) {
                startTime = rs.getString("data_inicio")
            }
            rs.close()
            getStmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (startTime == null) {
            connection.close()
            return SessionEndResponse(
                success = false,
                message = "Sessão não encontrada"
            )
        }

        val updateSql = "UPDATE sessoes SET status = 'finalizada', data_fim = NOW() WHERE id_sessao = ?"

        try {
            val stmt = connection.prepareStatement(updateSql)
            stmt.setInt(1, sessionDbId)
            stmt.executeUpdate()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            return SessionEndResponse(
                success = false,
                message = "Erro ao finalizar sessão"
            )
        }

        val startInstant = Instant.parse(startTime)
        val endInstant = Instant.now()
        val durationSeconds = ChronoUnit.SECONDS.between(startInstant, endInstant)

        connection.close()

        return SessionEndResponse(
            success = true,
            data = CompletedSessionData(
                sessionId = sessionId,
                duration = durationSeconds,
                productsScanned = 0,
                efficiency = 100f
            ),
            message = "Sessão finalizada com sucesso"
        )
    }

    fun endSessionById(request: SessionEndRequest): SessionEndResponse {
        return endSessionById(request.sessionId)
    }

    fun getUserHistory(userId: String, limit: Int = 20): UserSessionHistoryResponse {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT 
                s.id_sessao as sessionId,
                s.data_inicio as startTime,
                s.data_fim as endTime,
                e.nome as machineName,
                s.status
            FROM sessoes s
            LEFT JOIN equipes e ON s.id_equipe = e.id_equipe
            WHERE s.id_usuario = ?
            ORDER BY s.data_inicio DESC
            LIMIT ?
        """.trimIndent()

        val history = mutableListOf<UserSessionRecord>()

        try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, userId.toInt())
            stmt.setInt(2, limit)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val startTime = rs.getString("startTime")
                val endTime = rs.getString("endTime")
                val duration = if (endTime != null) {
                    ChronoUnit.SECONDS.between(Instant.parse(startTime), Instant.parse(endTime))
                } else null

                history.add(
                    UserSessionRecord(
                        id = rs.getString("sessionId"),
                        sessionId = rs.getString("sessionId"),
                        qrCode = "",
                        machineName = rs.getString("machineName") ?: "Máquina",
                        sessionType = "WORK",
                        startTime = startTime,
                        endTime = endTime,
                        duration = duration,
                        status = rs.getString("status"),
                        productsScanned = 0
                    )
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        connection.close()

        return UserSessionHistoryResponse(
            success = true,
            data = history,
            total = history.size
        )
    }

    fun getSessionStats(userId: String): Map<String, Any> {
        val history = getUserHistory(userId).data
        val totalSessions = history.size
        val totalProductsScanned = history.sumOf { it.productsScanned }
        val avgDuration = if (history.isNotEmpty()) {
            history.mapNotNull { it.duration }.average().toLong()
        } else 0

        val activeSession = getActiveSession(userId)
        val hasActiveSession = activeSession.success

        return mapOf(
            "totalSessions" to totalSessions,
            "totalProductsScanned" to totalProductsScanned,
            "averageSessionDuration" to avgDuration,
            "activeSession" to hasActiveSession
        )
    }
}