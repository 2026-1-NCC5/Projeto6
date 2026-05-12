// data/api/SessionApi.kt
package com.AlimempatIA.stockai.data.api

import retrofit2.http.*
import com.AlimempatIA.stockai.data.api.dto.*

interface SessionApi {

    @POST("sessions/start")
    suspend fun startSession(@Body request: SessionRequest): SessionResponse

    @GET("sessions/active/{userId}")
    suspend fun getActiveSession(@Path("userId") userId: String): SessionResponse

    @POST("sessions/end")
    suspend fun endSession(@Body request: SessionEndRequest): SessionEndResponse

    @GET("sessions/history/{userId}")
    suspend fun getSessionHistory(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 20
    ): UserSessionHistoryResponse

    @GET("sessions/stats/{userId}")
    suspend fun getSessionStats(@Path("userId") userId: String): Map<String, Any>
}