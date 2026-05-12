package com.AlimempatIA.stockai.domain.model

import androidx.compose.ui.graphics.Color

data class AttributeScore(
    val name: String,
    val score: Int,
    val previousScore: Int,
    val color: Color
)

data class MemberRecord(
    val id: String,
    val name: String,
    val role: String,
    val roleColor: Color,
    val joinDate: String,
    val attributes: List<AttributeScore>,
    val weeklyEvolution: List<Int>,
    val monthlyEvolution: List<Int>,
    val trend: Trend,
    val leaderNote: String,
    val actionPlan: String
) {
    val currentScore: Int get() = if (attributes.isNotEmpty()) {
        attributes.map { it.score }.average().toInt()
    } else 0

    val previousScore: Int get() = if (attributes.isNotEmpty()) {
        attributes.map { it.previousScore }.average().toInt()
    } else 0

    val scoreDelta: Int get() = currentScore - previousScore

    val strongestAttribute: AttributeScore get() = attributes.maxByOrNull { it.score }
        ?: AttributeScore("", 0, 0, Color.Transparent)

    val developmentAttribute: AttributeScore get() = attributes.minByOrNull { it.score }
        ?: AttributeScore("", 0, 0, Color.Transparent)
}

enum class Trend {
    UP, STABLE, DOWN
}