package com.AlimempatIA.stockai.data.api.dto


data class InventoryResponse(
    val cameraStatus: CameraStatusDto,
    val stats: InventoryStatsDto,
    val availableCategories: List<String>,
    val availableStatuses: List<String>,
    val products: List<ProductDto>
)

data class CameraStatusDto(
    val id: String,
    val name: String,
    val location: String,
    val status: String
)

data class InventoryStatsDto(
    val total: Int,
    val inStock: Int,
    val lowStock: Int,
    val outOfStock: Int
)

data class ProductDetailResponse(
    val product: ProductDto,
    val stockLevel: StockLevelDto,
    val aiDetection: AiDetectionDto
)

data class StockLevelDto(
    val current: Int,
    val minimum: Int,
    val progress: Float
)

data class AiDetectionDto(
    val cameraId: String,
    val cameraName: String,
    val location: String,
    val confidence: Double,
    val detectedAt: String
)


data class UpdateProductRequest(
    val name: String,
    val sku: String,
    val category: String,
    val minStock: Int
)

data class AdjustStockRequest(
    val quantity: Int,
    val reason: String
)