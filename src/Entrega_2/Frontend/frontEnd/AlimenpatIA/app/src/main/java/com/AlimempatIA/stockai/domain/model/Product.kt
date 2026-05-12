package com.AlimempatIA.stockai.domain.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val category: ProductCategory,
    val quantity: Int,
    val minStock: Int,
    val drawableResId: Int? = null,
    val detectedAt: LocalDateTime,
    val status: StockStatus
) {
    val formattedDetectedTime: String
        get() = detectedAt.format(DateTimeFormatter.ofPattern("HH:mm"))

    val formattedDetectedDate: String
        get() = detectedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

enum class ProductCategory(val label: String) {
    CEREAIS("Cereais"),
    LEGUMINOSAS("Leguminosas"),
    MASSAS("Massas"),
    OLEOS("Óleos"),
    LATICINIOS("Laticínios");

    companion object {
        fun fromString(name: String): ProductCategory {
            return values().find { it.name == name } ?: CEREAIS
        }
    }
}

enum class StockStatus(val label: String) {
    IN_STOCK("Em Estoque"),
    LOW_STOCK("Estoque Baixo"),
    OUT_OF_STOCK("Sem Estoque");

    companion object {
        fun fromString(name: String): StockStatus {
            return values().find { it.name == name } ?: IN_STOCK
        }
    }
}