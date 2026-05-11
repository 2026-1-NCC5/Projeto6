package com.AlimempatIA.stockai.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import retrofit2.http.GET
import com.AlimempatIA.stockai.data.api.dto.ProductDto
import com.AlimempatIA.stockai.data.api.dto.DashboardStatsResponse

object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val productApi: ProductApi by lazy {
        retrofit.create(ProductApi::class.java)
    }

    val dashboardApi: DashboardApi by lazy {
        retrofit.create(DashboardApi::class.java)
    }

    val reportsApi: ReportsApi by lazy {
        retrofit.create(ReportsApi::class.java)
    }

    val sessionApi: SessionApi by lazy {
        retrofit.create(SessionApi::class.java)
    }

    interface UnifiedApi {
        @GET("products")
        suspend fun getProducts(): List<ProductDto>

        @GET("dashboard/stats")
        suspend fun getDashboardStats(): DashboardStatsResponse
    }

    val api: UnifiedApi by lazy {
        retrofit.create(UnifiedApi::class.java)
    }
}