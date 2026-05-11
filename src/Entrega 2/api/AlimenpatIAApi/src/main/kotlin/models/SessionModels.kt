package com.alimenpatia.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.UUID

@Serializable
data class SessionRequest(
    val qrCode: String,
    val userId: String,
    val userName: String,
    val sessionType: SessionType,
    val startTime: String = Instant.now().toString()
)

@Serializable
enum class SessionType(val label: String) {
    WORK("Trabalho"),
    TRAINING("Treinamento"),
    MAINTENANCE("Manutenção"),
    INSPECTION("Inspeção")
}

@Serializable
data class SessionResponse(
    val success: Boolean,
    val data: SessionData? = null,
    val message: String? = null
)

@Serializable
data class SessionData(
    val sessionId: String,
    val qrCode: String,
    val userId: String,
    val userName: String,
    val sessionType: SessionType,
    val startTime: String,
    val status: SessionStatus,
    val machineInfo: MachineInfo? = null
)

@Serializable
enum class SessionStatus(val label: String) {
    ACTIVE("Ativa"),
    COMPLETED("Completada"),
    EXPIRED("Expirada"),
    CANCELLED("Cancelada")
}

@Serializable
data class SessionEndRequest(
    val sessionId: String,
    val endTime: String = Instant.now().toString(),
    val notes: String? = null,
    val productsScanned: Int = 0
)

@Serializable
data class SessionEndResponse(
    val success: Boolean,
    val data: CompletedSessionData? = null,
    val message: String? = null
)

@Serializable
data class CompletedSessionData(
    val sessionId: String,
    val duration: Long, // em segundos
    val productsScanned: Int,
    val efficiency: Float,
    val totalValue: Float? = null
)

@Serializable
data class WebhookPayload(
    val event: String,
    val sessionId: String,
    val qrCode: String,
    val userId: String,
    val userName: String,
    val timestamp: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class WebhookResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class UserSessionHistoryResponse(
    val success: Boolean,
    val data: List<UserSessionRecord> = emptyList(),
    val total: Int = 0,
    val message: String? = null
)

@Serializable
data class UserSessionRecord(
    val id: String,
    val sessionId: String,
    val qrCode: String,
    val machineName: String,
    val sessionType: String,
    val startTime: String,
    val endTime: String? = null,
    val duration: Long? = null,
    val status: String,
    val productsScanned: Int
)