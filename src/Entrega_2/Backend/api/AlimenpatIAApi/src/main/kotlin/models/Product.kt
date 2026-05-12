package com.alimenpatia.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Product(
    val id: String,
    val name: String,
    val sku: String = "",
    val category: ProductCategory,
    val quantity: Int,
    val minStock: Int = 0,
    val drawableResId: Int? = null,
    val detectedAt: String = Instant.now().toString()
) {
    val status: StockStatus
        get() = when {
            quantity == 0 -> StockStatus.OUT_OF_STOCK
            quantity <= minStock -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}

@Serializable
enum class ProductCategory(val label: String) {
    CEREAIS("Cereais"),
    LEGUMINOSAS("Leguminosas"),
    MASSAS("Massas"),
    OLEOS("Óleos"),
    LATICINIOS("Laticínios")
}

@Serializable
enum class StockStatus(val label: String) {
    IN_STOCK("Em Estoque"),
    LOW_STOCK("Estoque Baixo"),
    OUT_OF_STOCK("Sem Estoque")
}