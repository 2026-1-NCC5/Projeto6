package com.AlimempatIA.stockai.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.AlimempatIA.stockai.data.api.dto.*

interface DashboardApi {

    @GET("dashboard/stats")
    suspend fun getDashboardStats(): DashboardStatsResponse

}