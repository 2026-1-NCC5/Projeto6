package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.Instant
import java.util.UUID

object InventoryDatabase {

    private val products = mutableListOf<Product>()
    private var currentProductId = 100

    init {
        generateMockProducts()
    }

    private fun generateMockProducts() {
        products.clear()
        products.addAll(
            listOf(
                Product(
                    id = "prd_001",
                    name = "Arroz Branco 5kg",
                    sku = "ARR-5KG-001",
                    category = ProductCategory.CEREAIS,
                    quantity = 42,
                    minStock = 20,
                    detectedAt = "2026-05-03T19:42:00"
                ),
                Product(
                    id = "prd_002",
                    name = "Feijao Carioca 1kg",
                    sku = "FEI-1KG-002",
                    category = ProductCategory.LEGUMINOSAS,
                    quantity = 8,
                    minStock = 15,
                    detectedAt = "2026-05-03T19:38:00"
                ),
                Product(
                    id = "prd_003",
                    name = "Macarrao Espaguete 500g",
                    sku = "MAC-500G-003",
                    category = ProductCategory.MASSAS,
                    quantity = 0,
                    minStock = 10,
                    detectedAt = "2026-05-02T10:15:00"
                ),
                Product(
                    id = "prd_004",
                    name = "Azeite de Oliva 500ml",
                    sku = "AZE-500ML-004",
                    category = ProductCategory.OLEOS,
                    quantity = 15,
                    minStock = 5,
                    detectedAt = "2026-05-03T14:20:00"
                ),
                Product(
                    id = "prd_005",
                    name = "Leite UHT 1L",
                    sku = "LEI-1L-005",
                    category = ProductCategory.LATICINIOS,
                    quantity = 3,
                    minStock = 10,
                    detectedAt = "2026-05-01T09:30:00"
                )
            )
        )
        currentProductId = 106
    }

    fun getAllProducts(
        search: String? = null,
        category: String? = null,
        status: String? = null
    ): List<Product> {
        var filtered = products.toList()

        // 过滤搜索词
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter {
                it.name.contains(search, ignoreCase = true) ||
                        it.sku.contains(search, ignoreCase = true)
            }
        }

        // 过滤分类
        if (!category.isNullOrBlank()) {
            filtered = filtered.filter { it.category.name == category }
        }

        // 过滤状态
        if (!status.isNullOrBlank()) {
            filtered = filtered.filter { it.status.name == status }
        }

        return filtered
    }

    fun getProductById(id: String): Product? {
        return products.find { it.id == id }
    }

    fun updateProduct(id: String, request: UpdateProductRequest): Boolean {
        val index = products.indexOfFirst { it.id == id }
        if (index == -1) return false

        val oldProduct = products[index]
        val newCategory = try {
            ProductCategory.valueOf(request.category)
        } catch (e: IllegalArgumentException) {
            oldProduct.category
        }

        products[index] = oldProduct.copy(
            name = request.name,
            sku = request.sku,
            category = newCategory,
            minStock = request.minStock
        )
        return true
    }

    fun adjustStock(id: String, quantity: Int): Boolean {
        val index = products.indexOfFirst { it.id == id }
        if (index == -1) return false

        val oldProduct = products[index]
        val newQuantity = (oldProduct.quantity + quantity).coerceAtLeast(0)

        products[index] = oldProduct.copy(
            quantity = newQuantity,
            detectedAt = Instant.now().toString()
        )
        return true
    }

    fun getCameraStatus(): CameraStatus {
        return CameraStatus(
            id = "cam_001",
            name = "Camera 01",
            location = "Esteira Principal",
            status = "DETECTING"
        )
    }

    fun getInventoryStats(products: List<Product>): InventoryStats {
        return InventoryStats(
            total = products.size,
            inStock = products.count { it.status == StockStatus.IN_STOCK },
            lowStock = products.count { it.status == StockStatus.LOW_STOCK },
            outOfStock = products.count { it.status == StockStatus.OUT_OF_STOCK }
        )
    }

    fun getAvailableCategories(): List<String> {
        return ProductCategory.entries.map { it.name }
    }

    fun getAvailableStatuses(): List<String> {
        return StockStatus.entries.map { it.name }
    }

    fun getStockLevel(product: Product): StockLevel {
        val progress = if (product.minStock > 0) {
            (product.quantity.toFloat() / (product.minStock * 3).toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        return StockLevel(
            current = product.quantity,
            minimum = product.minStock,
            progress = progress
        )
    }

    fun getAiDetection(product: Product): AiDetection {
        return AiDetection(
            cameraId = "cam_001",
            cameraName = "Camera 01",
            location = "Esteira Principal",
            confidence = 98.4,
            detectedAt = product.detectedAt
        )
    }
}