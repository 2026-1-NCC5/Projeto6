package com.alimenpatia.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardStatsResponse(
    val success: Boolean,
    val data: DashboardStatsData?,
    val message: String? = null
)

@Serializable
data class DashboardStatsData(
    val recognized: Int,
    val notRecognized: Int,
    val totalTeam: Int,
    val weeklyData: WeeklyDataDto,
    val goal: GoalInfoDto
)

@Serializable
data class WeeklyDataDto(
    val dailyCounts: List<DailyCountDto>,
    val cumulativeCounts: List<DailyCountDto>,
    val weekDays: List<String>
)

@Serializable
data class DailyCountDto(
    val day: String,
    val date: String,
    val count: Int
)

@Serializable
data class GoalInfoDto(
    val current: Int,
    val target: Int,
    val percentage: Int
)

@Serializable
data class WeeklyDataResponse(
    val success: Boolean,
    val data: WeeklyDataDto?,
    val message: String? = null
)

@Serializable
data class GoalProgressResponse(
    val success: Boolean,
    val data: GoalInfoDto?,
    val message: String? = null
)

@Serializable
data class RecognitionStatsResponse(
    val success: Boolean,
    val data: RecognitionStatsDto?,
    val message: String? = null
)

@Serializable
data class RecognitionStatsDto(
    val totalScans: Int,
    val recognizedCount: Int,
    val notRecognizedCount: Int,
    val recognitionRate: Float,
    val dailyAverage: Int
)

@Serializable
data class ProductRecognitionResponse(
    val success: Boolean,
    val data: List<ProductRecognitionDto>?,
    val total: Int? = null,
    val message: String? = null
)

@Serializable
data class ProductRecognitionDto(
    val id: String,
    val name: String,
    val wasRecognized: Boolean,
    val recognizedAt: String? = null,
    val confidence: Float? = null
)