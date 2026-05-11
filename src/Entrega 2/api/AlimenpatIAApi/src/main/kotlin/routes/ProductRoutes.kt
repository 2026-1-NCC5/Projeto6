package com.alimenpatia.routes

import com.alimenpatia.models.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes() {

    // GET /api/inventory
    get("/inventory") {
        val search = call.request.queryParameters["search"]
        val category = call.request.queryParameters["category"]
        val status = call.request.queryParameters["status"]

        val allProducts = getMockProducts()

        var filtered = allProducts
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter {
                it.name.contains(search, ignoreCase = true) ||
                        it.sku.contains(search, ignoreCase = true)
            }
        }
        if (!category.isNullOrBlank()) {
            filtered = filtered.filter { it.category.name == category }
        }
        if (!status.isNullOrBlank()) {
            filtered = filtered.filter { it.status.name == status }
        }

        val stats = InventoryStats(
            total = allProducts.size,
            inStock = allProducts.count { it.status == StockStatus.IN_STOCK },
            lowStock = allProducts.count { it.status == StockStatus.LOW_STOCK },
            outOfStock = allProducts.count { it.status == StockStatus.OUT_OF_STOCK }
        )

        val response = InventoryResponse(
            cameraStatus = CameraStatus(
                id = "cam_001",
                name = "Camera 01",
                location = "Esteira Principal",
                status = "DETECTING"
            ),
            stats = stats,
            availableCategories = ProductCategory.entries.map { it.name },
            availableStatuses = StockStatus.entries.map { it.name },
            products = filtered
        )

        call.respond(response)
    }

    // GET /api/inventory/{productId}
    get("/inventory/{productId}") {
        val productId = call.parameters["productId"]
        val product = getMockProducts().find { it.id == productId }

        if (product != null) {
            val response = mapOf(
                "product" to product,
                "stockLevel" to mapOf(
                    "current" to product.quantity,
                    "minimum" to product.minStock,
                    "progress" to (product.quantity.toFloat() / (product.minStock * 3).toFloat()).coerceIn(0f, 1f)
                ),
                "aiDetection" to mapOf(
                    "cameraId" to "cam_001",
                    "cameraName" to "Camera 01",
                    "location" to "Esteira Principal",
                    "confidence" to 98.4,
                    "detectedAt" to product.detectedAt
                )
            )
            call.respond(response)
        } else {
            call.respond(mapOf("error" to "Product not found"))
        }
    }

    // PUT /api/inventory/{productId}
    put("/inventory/{productId}") {
        call.respond(mapOf("success" to true, "message" to "Product updated"))
    }

    // POST /api/inventory/{productId}/adjust-stock
    post("/inventory/{productId}/adjust-stock") {
        call.respond(mapOf("success" to true, "message" to "Stock adjusted"))
    }

    get("/products") {
        call.respond(getMockProducts())
    }
}

private fun getMockProducts(): List<Product> {
    return listOf(
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
}