package com.alimenpatia.database

import com.alimenpatia.models.*

object ReportsDatabase {

    private val members = mutableListOf<MemberDataDto>()

    init {
        generateMockMembers()
    }

    private fun generateMockMembers() {
        members.clear()
        members.addAll(
            listOf(
                MemberDataDto(
                    id = "mbr_001",
                    name = "Carlos Mendes",
                    role = "OPERADOR",
                    roleColor = "#1A73E8",
                    joinDate = "2026-01-10",
                    trend = "UP",
                    currentScore = 84,
                    previousScore = 80,
                    scoreDelta = 4,
                    leaderNote = "Ganhou velocidade sem perder qualidade nas conferencias.",
                    actionPlan = "Dar mais autonomia em fechamento de lotes acompanhados.",
                    attributes = listOf(
                        AttributeDto("Precisao", 92, 88, "#00E676"),
                        AttributeDto("Agilidade", 86, 79, "#1A73E8"),
                        AttributeDto("Organizacao", 81, 80, "#00BCD4"),
                        AttributeDto("Autonomia", 78, 72, "#7C4DFF")
                    ),
                    weeklyEvolution = listOf(72, 76, 78, 81, 84, 86),
                    monthlyEvolution = listOf(68, 71, 74, 79, 83, 86)
                ),
                MemberDataDto(
                    id = "mbr_002",
                    name = "Patricia Lima",
                    role = "SUPERVISAO",
                    roleColor = "#00BCD4",
                    joinDate = "2026-02-05",
                    trend = "UP",
                    currentScore = 92,
                    previousScore = 88,
                    scoreDelta = 4,
                    leaderNote = "Assume bem a orientacao do grupo e ajuda operadores novos.",
                    actionPlan = "Registrar boas praticas para padronizar treinamentos.",
                    attributes = listOf(
                        AttributeDto("Lideranca", 94, 91, "#FFC107"),
                        AttributeDto("Comunicacao", 90, 87, "#00BCD4"),
                        AttributeDto("Precisao", 95, 93, "#00E676"),
                        AttributeDto("Apoio ao time", 91, 88, "#7C4DFF")
                    ),
                    weeklyEvolution = listOf(84, 86, 88, 90, 91, 92),
                    monthlyEvolution = listOf(80, 83, 86, 88, 90, 92)
                ),
                MemberDataDto(
                    id = "mbr_003",
                    name = "Roberto Souza",
                    role = "OPERADOR",
                    roleColor = "#1A73E8",
                    joinDate = "2026-01-20",
                    trend = "DOWN",
                    currentScore = 69,
                    previousScore = 74,
                    scoreDelta = -5,
                    leaderNote = "Mostra queda em rotina e precisa de acompanhamento mais proximo.",
                    actionPlan = "Revisar checklist diario e fazer feedback curto ao fim do turno.",
                    attributes = listOf(
                        AttributeDto("Precisao", 72, 78, "#FFC107"),
                        AttributeDto("Agilidade", 70, 76, "#1A73E8"),
                        AttributeDto("Organizacao", 66, 73, "#F44336"),
                        AttributeDto("Autonomia", 69, 71, "#7C4DFF")
                    ),
                    weeklyEvolution = listOf(76, 74, 72, 70, 68, 69),
                    monthlyEvolution = listOf(78, 76, 74, 72, 70, 69)
                ),
                MemberDataDto(
                    id = "mbr_004",
                    name = "Ana Ferreira",
                    role = "COORDENACAO",
                    roleColor = "#FFD600",
                    joinDate = "2026-01-05",
                    trend = "STABLE",
                    currentScore = 89,
                    previousScore = 89,
                    scoreDelta = 0,
                    leaderNote = "Mantem padrao alto e previsivel nas entregas do time.",
                    actionPlan = "Criar metas de desenvolvimento para ampliar delegacao.",
                    attributes = listOf(
                        AttributeDto("Consistencia", 93, 92, "#00E676"),
                        AttributeDto("Lideranca", 88, 88, "#FFC107"),
                        AttributeDto("Comunicacao", 86, 85, "#00BCD4"),
                        AttributeDto("Organizacao", 91, 90, "#1A73E8")
                    ),
                    weeklyEvolution = listOf(87, 88, 88, 89, 89, 89),
                    monthlyEvolution = listOf(84, 86, 87, 88, 89, 89)
                ),
                MemberDataDto(
                    id = "mbr_005",
                    name = "Marcos Oliveira",
                    role = "OPERADOR",
                    roleColor = "#1A73E8",
                    joinDate = "2026-03-01",
                    trend = "UP",
                    currentScore = 80,
                    previousScore = 68,
                    scoreDelta = 12,
                    leaderNote = "Evolucao rapida depois da adaptacao inicial.",
                    actionPlan = "Manter dupla com operador experiente por mais duas semanas.",
                    attributes = listOf(
                        AttributeDto("Aprendizado", 84, 68, "#7C4DFF"),
                        AttributeDto("Agilidade", 78, 63, "#1A73E8"),
                        AttributeDto("Precisao", 80, 70, "#00E676"),
                        AttributeDto("Organizacao", 76, 65, "#00BCD4")
                    ),
                    weeklyEvolution = listOf(61, 66, 70, 74, 77, 80),
                    monthlyEvolution = listOf(55, 61, 67, 72, 77, 80)
                )
            )
        )
    }

    fun getAllMembers(role: String? = null, trend: String? = null): List<MemberDataDto> {
        var filtered = members.toList()
        if (!role.isNullOrBlank()) {
            filtered = filtered.filter { it.role == role }
        }
        if (!trend.isNullOrBlank()) {
            filtered = filtered.filter { it.trend.uppercase() == trend.uppercase() }
        }
        return filtered
    }

    fun getMemberById(id: String): MemberDataDto? = members.find { it.id == id }

    fun getRoles(): List<String> = members.map { it.role }.distinct()

    fun calculateSummary(members: List<MemberDataDto>): ReportsSummary {
        if (members.isEmpty()) {
            return ReportsSummary(0, 0, 0, "")
        }
        val teamAttributeAverage = members.map { it.currentScore }.average().toInt()
        val averageDelta = members.map { it.scoreDelta }.average().toInt()
        val attentionCount = members.count { it.currentScore < 75 || it.trend == "DOWN" }
        val bestEvolutionMemberId = members.maxByOrNull { it.scoreDelta }?.id ?: ""
        return ReportsSummary(teamAttributeAverage, averageDelta, attentionCount, bestEvolutionMemberId)
    }

    fun calculateAttributeMap(members: List<MemberDataDto>): List<AttributeMapDto> {
        if (members.isEmpty()) return emptyList()
        val allAttributes = members.flatMap { member ->
            member.attributes.map { Triple(it.name, it.score, it.previousScore) }
        }
        val grouped = allAttributes.groupBy { it.first }
        return grouped.map { (name, values) ->
            AttributeMapDto(
                name = name,
                score = values.map { it.second }.average().toInt(),
                previousScore = values.map { it.third }.average().toInt(),
                color = values.firstOrNull()?.let {
                    when (name.lowercase()) {
                        "precisao" -> "#00E676"
                        "agilidade" -> "#1A73E8"
                        "organizacao" -> "#00BCD4"
                        "autonomia" -> "#7C4DFF"
                        "lideranca" -> "#FFC107"
                        else -> "#1A73E8"
                    }
                } ?: "#1A73E8"
            )
        }.sortedByDescending { it.score }
    }

    fun calculateTrendCounts(members: List<MemberDataDto>): TrendCountsDto {
        return TrendCountsDto(
            evolving = members.count { it.trend.uppercase() == "UP" },
            maintaining = members.count { it.trend.uppercase() == "STABLE" },
            attention = members.count { it.trend.uppercase() == "DOWN" }
        )
    }

    fun getReportsStats(): ReportsStatsData {
        val totalProduction = members.sumOf { it.weeklyEvolution.sum() }
        val avgAccuracy = if (members.isNotEmpty()) {
            members.flatMap { it.attributes.map { attr -> attr.score } }.average().toInt()
        } else 0
        val maxStreak = if (members.isNotEmpty()) {
            members.flatMap { it.attributes.map { attr -> attr.score } }.maxOrNull() ?: 0
        } else 0
        val membersInUp = members.count { it.trend.uppercase() == "UP" }
        return ReportsStatsData(totalProduction, avgAccuracy, maxStreak, membersInUp, members.size)
    }
}