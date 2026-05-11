package com.AlimempatIA.stockai.data.api.dto


data class ReportsScreenResponse(
    val summary: ReportsSummary,
    val attributeMap: List<AttributeMapDto>,
    val trendCounts: TrendCountsDto,
    val members: List<MemberDataDto>
)

data class ReportsSummary(
    val teamAttributeAverage: Int,
    val averageDelta: Int,
    val attentionCount: Int,
    val bestEvolutionMemberId: String
)

data class AttributeMapDto(
    val name: String,
    val score: Int,
    val previousScore: Int,
    val color: String
)

data class TrendCountsDto(
    val evolving: Int,
    val maintaining: Int,
    val attention: Int
)

data class MemberDataDto(
    val id: String,
    val name: String,
    val role: String,
    val roleColor: String,
    val joinDate: String,
    val trend: String,
    val currentScore: Int,
    val previousScore: Int,
    val scoreDelta: Int,
    val leaderNote: String,
    val actionPlan: String,
    val attributes: List<AttributeDto>,
    val weeklyEvolution: List<Int>,
    val monthlyEvolution: List<Int>
)

data class AttributeDto(
    val name: String,
    val score: Int,
    val previousScore: Int,
    val color: String
)


data class ReportsStatsResponse(
    val success: Boolean,
    val data: ReportsStatsData?,
    val message: String? = null
)

data class ReportsStatsData(
    val totalProduction: Int,
    val avgAccuracy: Int,
    val maxStreak: Int,
    val membersInUp: Int,
    val totalMembers: Int
)

data class MemberListResponse(
    val success: Boolean,
    val data: List<MemberDataDto>?,
    val total: Int = 0,
    val message: String? = null
)

data class MemberDetailResponse(
    val success: Boolean,
    val data: MemberDataDto?,
    val message: String? = null
)

data class RolesResponse(
    val success: Boolean,
    val data: List<String>?,
    val message: String? = null
)