package com.alimenpatia.models

import kotlinx.serialization.Serializable


@Serializable
data class InventoryResponse(
    val cameraStatus: CameraStatus,
    val stats: InventoryStats,
    val availableCategories: List<String>,
    val availableStatuses: List<String>,
    val products: List<Product>
)

@Serializable
data class CameraStatus(
    val id: String,
    val name: String,
    val location: String,
    val status: String
)

@Serializable
data class InventoryStats(
    val total: Int,
    val inStock: Int,
    val lowStock: Int,
    val outOfStock: Int
)


@Serializable
data class ProductDetailResponse(
    val product: Product,
    val stockLevel: StockLevel,
    val aiDetection: AiDetection
)

@Serializable
data class StockLevel(
    val current: Int,
    val minimum: Int,
    val progress: Float
)

@Serializable
data class AiDetection(
    val cameraId: String,
    val cameraName: String,
    val location: String,
    val confidence: Double,
    val detectedAt: String
)


@Serializable
data class UpdateProductRequest(
    val name: String,
    val sku: String,
    val category: String,
    val minStock: Int
)

@Serializable
data class AdjustStockRequest(
    val quantity: Int,
    val reason: String
)