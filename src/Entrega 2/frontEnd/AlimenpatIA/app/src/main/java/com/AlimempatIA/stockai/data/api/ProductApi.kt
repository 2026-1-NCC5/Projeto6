package com.AlimempatIA.stockai.data.api

import retrofit2.http.*
import com.AlimempatIA.stockai.data.api.dto.*

interface ProductApi {

    @GET("inventory")
    suspend fun getInventory(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null
    ): InventoryResponse

    @GET("inventory/{productId}")
    suspend fun getProductDetail(@Path("productId") productId: String): ProductDetailResponse


    @PUT("inventory/{productId}")
    suspend fun updateProduct(
        @Path("productId") productId: String,
        @Body request: UpdateProductRequest
    ): Map<String, Any>

    @POST("inventory/{productId}/adjust-stock")
    suspend fun adjustStock(
        @Path("productId") productId: String,
        @Body request: AdjustStockRequest
    ): Map<String, Any>


    @GET("products")
    suspend fun getProducts(): List<ProductDto>
}