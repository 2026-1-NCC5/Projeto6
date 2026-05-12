package com.alimenpatia.models

import kotlinx.serialization.Serializable


@Serializable
data class QRScanRequest(
    val qrCode: String
)

@Serializable
data class QRScanResponse(
    val success: Boolean,
    val data: MachineInfo? = null,
    val message: String? = null
)


@Serializable
data class MachineInfo(
    val id: String,
    val name: String,
    val code: String,
    val location: String,
    val status: String,
    val lastScan: String? = null,
    val productCount: Int = 0,
    val products: List<ProductSummary> = emptyList()
)

@Serializable
data class ProductSummary(
    val id: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val status: String
)


@Serializable
data class MachineStockResponse(
    val success: Boolean,
    val data: MachineStock? = null,
    val message: String? = null
)

@Serializable
data class MachineStock(
    val machineId: String,
    val machineName: String,
    val machineLocation: String,
    val totalProducts: Int,
    val inStockCount: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val products: List<ProductStockDetail>
)

@Serializable
data class ProductStockDetail(
    val id: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val minStock: Int,
    val status: String
)


@Serializable
data class ScanHistoryResponse(
    val success: Boolean,
    val data: List<ScanRecord> = emptyList(),
    val total: Int = 0,
    val message: String? = null
)

@Serializable
data class ScanRecord(
    val id: String,
    val qrCode: String,
    val userId: String? = null,
    val scannedAt: String,
    val machineId: String,
    val machineName: String,
    val result: String
)

enum class ScanResult {
    SUCCESS,
    INVALID_QR,
    MACHINE_OFFLINE,
    ALREADY_SCANNED
}


enum class MachineStatus(val label: String) {
    ONLINE("Online"),
    OFFLINE("Offline"),
    MAINTENANCE("Manutenção"),
    DETECTING("Detectando")
}


@Serializable
data class CameraMachinesResponse(
    val success: Boolean,
    val data: List<MachineInfo>,
    val total: Int
)