package com.AlimempatIA.stockai.data.api.dto

data class DashboardStatsResponse(
    val success: Boolean,
    val data: DashboardStatsData?,
    val message: String? = null
)

data class DashboardStatsData(
    val recognized: Int,
    val notRecognized: Int,
    val totalTeam: Int,
    val weeklyData: WeeklyDataDto,
    val goal: GoalInfoDto
)

data class WeeklyDataDto(
    val dailyCounts: List<DailyCountDto>,
    val cumulativeCounts: List<DailyCountDto>,
    val weekDays: List<String>
)

data class DailyCountDto(
    val day: String,
    val date: String,
    val count: Int
)

data class GoalInfoDto(
    val current: Int,
    val target: Int,
    val percentage: Int
)