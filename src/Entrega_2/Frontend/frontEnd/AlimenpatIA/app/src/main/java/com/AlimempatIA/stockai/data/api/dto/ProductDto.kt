package com.AlimempatIA.stockai.data.api.dto

data class ProductDto(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val quantity: Int,
    val minStock: Int,
    val drawableResId: Int? = null,
    val detectedAt: String
)