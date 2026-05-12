package com.AlimempatIA.stockai.data.mapper

import com.AlimempatIA.stockai.data.api.dto.ProductDto
import com.AlimempatIA.stockai.domain.model.Product
import com.AlimempatIA.stockai.domain.model.ProductCategory
import com.AlimempatIA.stockai.domain.model.StockStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun ProductDto.toDomain(): Product {
    return Product(
        id = id,
        name = name,
        sku = sku,
        category = ProductCategory.fromString(category),
        quantity = quantity,
        minStock = minStock,
        drawableResId = drawableResId,
        detectedAt = try {
            LocalDateTime.parse(detectedAt, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(detectedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            } catch (e2: Exception) {
                LocalDateTime.now()
            }
        },
        status = when {
            quantity == 0 -> StockStatus.OUT_OF_STOCK
            quantity <= minStock -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
    )
}