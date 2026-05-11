package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import com.alimenpatia.models.MachineStatus
import kotlin.math.min

object SessionDatabase {

    // 活跃会话存储
    private val activeSessions = mutableMapOf<String, SessionData>()

    // 已完成会话历史
    private val completedSessions = mutableListOf<UserSessionRecord>()

    // Webhook 配置
    var webhookUrl: String? = null
    var webhookEnabled: Boolean = true

    // 会话超时时间（分钟）
    const val SESSION_TIMEOUT_MINUTES = 60L

    fun createSession(request: SessionRequest): SessionResponse {
        // 检查用户是否已有活跃会话
        val existingSession = activeSessions.values.find {
            it.userId == request.userId && it.status == SessionStatus.ACTIVE
        }

        if (existingSession != null) {
            return SessionResponse(
                success = false,
                message = "Usuário já possui uma sessão ativa. Finalize-a antes de iniciar uma nova."
            )
        }

        // 验证 QR Code
        val machine = CameraDatabase.getMachineByQRCode(request.qrCode)
        if (machine == null) {
            return SessionResponse(
                success = false,
                message = "QR Code inválido. Máquina não encontrada."
            )
        }

        // 检查机器状态
        if (machine.status == MachineStatus.OFFLINE.label) {
            return SessionResponse(
                success = false,
                message = "Máquina está offline. Não é possível iniciar sessão."
            )
        }

        if (machine.status == MachineStatus.MAINTENANCE.label) {
            return SessionResponse(
                success = false,
                message = "Máquina em manutenção. Tente novamente mais tarde."
            )
        }

        val sessionId = UUID.randomUUID().toString()
        val session = SessionData(
            sessionId = sessionId,
            qrCode = request.qrCode,
            userId = request.userId,
            userName = request.userName,
            sessionType = request.sessionType,
            startTime = request.startTime,
            status = SessionStatus.ACTIVE,
            machineInfo = machine
        )

        activeSessions[sessionId] = session

        // 触发 Webhook
        triggerWebhook("session_started", session)

        // 记录扫描历史
        CameraDatabase.processScan(request.qrCode, request.userId)

        return SessionResponse(
            success = true,
            data = session,
            message = "Sessão iniciada com sucesso"
        )
    }

    fun getActiveSession(userId: String): SessionResponse {
        val session = activeSessions.values.find {
            it.userId == userId && it.status == SessionStatus.ACTIVE
        }

        // 检查会话是否超时
        if (session != null) {
            val startTime = Instant.parse(session.startTime)
            val now = Instant.now()
            val minutesElapsed = ChronoUnit.MINUTES.between(startTime, now)

            if (minutesElapsed >= SESSION_TIMEOUT_MINUTES) {
                // 自动过期会话
                endSessionById(
                    SessionEndRequest(
                        sessionId = session.sessionId,
                        notes = "Sessão expirada automaticamente após $SESSION_TIMEOUT_MINUTES minutos"
                    )
                )
                return SessionResponse(
                    success = false,
                    message = "Sessão expirada. Por favor, inicie uma nova sessão."
                )
            }
        }

        return if (session != null) {
            SessionResponse(
                success = true,
                data = session,
                message = "Sessão ativa encontrada"
            )
        } else {
            SessionResponse(
                success = false,
                message = "Nenhuma sessão ativa encontrada"
            )
        }
    }

    // 重命名函数避免冲突
    fun endSessionById(request: SessionEndRequest): SessionEndResponse {
        val session = activeSessions[request.sessionId]

        if (session == null) {
            return SessionEndResponse(
                success = false,
                message = "Sessão não encontrada"
            )
        }

        val startTime = Instant.parse(session.startTime)
        val endTime = Instant.parse(request.endTime)
        val durationSeconds = ChronoUnit.SECONDS.between(startTime, endTime)

        // 计算效率（基于扫描产品数量）
        val expectedProducts = session.machineInfo?.productCount ?: 1
        val efficiency = min(100f, (request.productsScanned.toFloat() / expectedProducts) * 100)

        // 移除活跃会话
        activeSessions.remove(request.sessionId)

        // 保存到历史记录
        val historyRecord = UserSessionRecord(
            id = UUID.randomUUID().toString(),
            sessionId = session.sessionId,
            qrCode = session.qrCode,
            machineName = session.machineInfo?.name ?: "Unknown",
            sessionType = session.sessionType.label,
            startTime = session.startTime,
            endTime = request.endTime,
            duration = durationSeconds,
            status = "COMPLETED",
            productsScanned = request.productsScanned
        )
        completedSessions.add(0, historyRecord)

        // 触发 Webhook - 使用 String 类型的 Map
        triggerWebhook("session_ended", session, mapOf(
            "duration" to durationSeconds.toString(),
            "productsScanned" to request.productsScanned.toString(),
            "efficiency" to efficiency.toString()
        ))

        return SessionEndResponse(
            success = true,
            data = CompletedSessionData(
                sessionId = session.sessionId,
                duration = durationSeconds,
                productsScanned = request.productsScanned,
                efficiency = efficiency,
                totalValue = null
            ),
            message = "Sessão finalizada com sucesso"
        )
    }

    fun getUserHistory(userId: String, limit: Int = 20): UserSessionHistoryResponse {
        val userHistory = completedSessions.take(limit)

        return UserSessionHistoryResponse(
            success = true,
            data = userHistory,
            total = userHistory.size
        )
    }

    private fun triggerWebhook(event: String, session: SessionData, extraData: Map<String, String> = emptyMap()) {
        if (!webhookEnabled || webhookUrl.isNullOrBlank()) {
            return
        }

        val payload = WebhookPayload(
            event = event,
            sessionId = session.sessionId,
            qrCode = session.qrCode,
            userId = session.userId,
            userName = session.userName,
            timestamp = Instant.now().toString(),
            data = mutableMapOf<String, String>().apply {
                put("sessionType", session.sessionType.name)
                put("machineName", session.machineInfo?.name ?: "")
                put("machineLocation", session.machineInfo?.location ?: "")
                putAll(extraData)
            }
        )

        // 异步发送 Webhook
        sendWebhookAsync(payload)
    }

    private fun sendWebhookAsync(payload: WebhookPayload) {
        // 这里应该使用 HTTP 客户端发送 POST 请求
        // 示例使用 ktor client
        println("Webhook triggered: ${webhookUrl}")
        println("Payload: $payload")
        // 实际实现：
        // val client = HttpClient()
        // client.post(webhookUrl!!) {
        //     contentType(ContentType.Application.Json)
        //     setBody(payload)
        // }
    }

    fun configureWebhook(url: String, enabled: Boolean = true) {
        webhookUrl = url
        webhookEnabled = enabled
    }

    fun getSessionStats(userId: String): Map<String, Any> {
        val userSessions = completedSessions

        val totalSessions = userSessions.size
        val totalProductsScanned = userSessions.sumOf { it.productsScanned }
        val avgDuration = if (userSessions.isNotEmpty()) {
            userSessions.mapNotNull { it.duration }.average().toLong()
        } else 0

        return mapOf(
            "totalSessions" to totalSessions,
            "totalProductsScanned" to totalProductsScanned,
            "averageSessionDuration" to avgDuration,
            "activeSession" to (activeSessions.values.find { it.userId == userId } != null)
        )
    }
}