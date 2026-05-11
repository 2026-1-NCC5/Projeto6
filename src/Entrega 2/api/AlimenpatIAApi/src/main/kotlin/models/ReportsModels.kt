package com.alimenpatia.models

import kotlinx.serialization.Serializable


@Serializable
data class ReportsScreenResponse(
    val summary: ReportsSummary,
    val attributeMap: List<AttributeMapDto>,
    val trendCounts: TrendCountsDto,
    val members: List<MemberDataDto>
)

@Serializable
data class ReportsSummary(
    val teamAttributeAverage: Int,
    val averageDelta: Int,
    val attentionCount: Int,
    val bestEvolutionMemberId: String
)

@Serializable
data class AttributeMapDto(
    val name: String,
    val score: Int,
    val previousScore: Int,
    val color: String
)

@Serializable
data class TrendCountsDto(
    val evolving: Int,
    val maintaining: Int,
    val attention: Int
)

@Serializable
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

@Serializable
data class AttributeDto(
    val name: String,
    val score: Int,
    val previousScore: Int,
    val color: String
)

@Serializable
data class ReportsStatsResponse(
    val success: Boolean,
    val data: ReportsStatsData? = null,
    val message: String? = null
)

@Serializable
data class ReportsStatsData(
    val totalProduction: Int,
    val avgAccuracy: Int,
    val maxStreak: Int,
    val membersInUp: Int,
    val totalMembers: Int
)

@Serializable
data class MemberListResponse(
    val success: Boolean,
    val data: List<MemberDataDto> = emptyList(),
    val total: Int = 0,
    val message: String? = null
)

@Serializable
data class MemberDetailResponse(
    val success: Boolean,
    val data: MemberDataDto? = null,
    val message: String? = null
)

@Serializable
data class RolesResponse(
    val success: Boolean,
    val data: List<String> = emptyList(),
    val message: String? = null
)