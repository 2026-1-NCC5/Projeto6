package com.AlimempatIA.stockai.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.AlimempatIA.stockai.data.api.dto.*

interface ReportsApi {

    @GET("reports/operator-attributes")
    suspend fun getOperatorAttributes(
        @Query("role") role: String? = null,
        @Query("trend") trend: String? = null,
        @Query("period") period: String = "weekly"
    ): ReportsScreenResponse

    @GET("reports/stats")
    suspend fun getReportsStats(): ReportsStatsResponse

    @GET("reports/members")
    suspend fun getMembers(
        @Query("role") role: String? = null,
        @Query("trend") trend: String? = null
    ): MemberListResponse

    @GET("reports/members/roles")
    suspend fun getRoles(): RolesResponse

    @GET("reports/members/{id}")
    suspend fun getMemberById(@Path("id") id: String): MemberDetailResponse
}