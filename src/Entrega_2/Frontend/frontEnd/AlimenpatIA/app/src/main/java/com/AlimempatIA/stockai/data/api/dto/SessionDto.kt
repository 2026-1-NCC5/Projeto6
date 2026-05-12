// data/api/dto/SessionDto.kt
package com.AlimempatIA.stockai.data.api.dto

import com.google.gson.annotations.SerializedName

data class SessionRequest(
    val qrCode: String,
    val userId: String,
    val userName: String,
    val sessionType: String,
    val startTime: String = java.time.Instant.now().toString()
)

data class SessionResponse(
    val success: Boolean,
    val data: SessionData?,
    val message: String?
)

data class SessionData(
    val sessionId: String,
    val qrCode: String,
    val userId: String,
    val userName: String,
    val sessionType: String,
    val startTime: String,
    val status: String,
    val machineInfo: MachineInfo?
)

data class MachineInfo(
    val id: String,
    val name: String,
    val code: String,
    val location: String,
    val status: String,
    val lastScan: String?,
    val productCount: Int,
    val products: List<ProductSummary> = emptyList()
)

data class ProductSummary(
    val id: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val status: String
)

data class SessionEndRequest(
    val sessionId: String,
    val endTime: String = java.time.Instant.now().toString(),
    val notes: String? = null,
    val productsScanned: Int = 0
)

data class SessionEndResponse(
    val success: Boolean,
    val data: CompletedSessionData?,
    val message: String?
)

data class CompletedSessionData(
    val sessionId: String,
    val duration: Long,
    val productsScanned: Int,
    val efficiency: Float,
    val totalValue: Float?
)

data class UserSessionHistoryResponse(
    val success: Boolean,
    val data: List<UserSessionRecord>,
    val total: Int,
    val message: String?
)

data class UserSessionRecord(
    val id: String,
    val sessionId: String,
    val qrCode: String,
    val machineName: String,
    val sessionType: String,
    val startTime: String,
    val endTime: String?,
    val duration: Long?,
    val status: String,
    val productsScanned: Int
)