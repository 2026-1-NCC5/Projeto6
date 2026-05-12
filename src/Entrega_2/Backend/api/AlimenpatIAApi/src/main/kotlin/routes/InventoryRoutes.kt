package com.alimenpatia.routes

import com.alimenpatia.database.InventoryDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.inventoryRoutes() {

    // GET /inventory
    get("/inventory") {
        val search = call.request.queryParameters["search"]
        val category = call.request.queryParameters["category"]
        val status = call.request.queryParameters["status"]

        val products = InventoryDatabaseMySQL.getAllProducts(search, category, status)
        val stats = InventoryDatabaseMySQL.getInventoryStats(products)
        val cameraStatus = InventoryDatabaseMySQL.getCameraStatus()

        val response = InventoryResponse(
            cameraStatus = cameraStatus,
            stats = stats,
            availableCategories = InventoryDatabaseMySQL.getAvailableCategories(),
            availableStatuses = InventoryDatabaseMySQL.getAvailableStatuses(),
            products = products
        )

        call.respond(response)
    }

    // GET /inventory/{productId}
    get("/inventory/{productId}") {
        val productId = call.parameters["productId"]

        if (productId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Product ID is required"))
            return@get
        }

        val product = InventoryDatabaseMySQL.getProductById(productId)

        if (product == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Product not found"))
            return@get
        }

        val response = ProductDetailResponse(
            product = product,
            stockLevel = InventoryDatabaseMySQL.getStockLevel(product),
            aiDetection = InventoryDatabaseMySQL.getAiDetection(product)
        )

        call.respond(response)
    }

    // PUT /inventory/{productId}
    put("/inventory/{productId}") {
        val productId = call.parameters["productId"]

        if (productId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Product ID is required"))
            return@put
        }

        val request = try {
            call.receive<UpdateProductRequest>()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Invalid request body"))
            return@put
        }

        val updated = InventoryDatabaseMySQL.updateProduct(productId, request)

        if (updated) {
            call.respond(mapOf("success" to true, "message" to "Product updated successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Product not found"))
        }
    }

    // POST /inventory/{productId}/adjust-stock
    post("/inventory/{productId}/adjust-stock") {
        val productId = call.parameters["productId"]

        if (productId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Product ID is required"))
            return@post
        }

        val request = try {
            call.receive<AdjustStockRequest>()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Invalid request body"))
            return@post
        }

        val adjusted = InventoryDatabaseMySQL.adjustStock(productId, request.quantity)

        if (adjusted) {
            call.respond(mapOf("success" to true, "message" to "Stock adjusted successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Product not found"))
        }
    }
}