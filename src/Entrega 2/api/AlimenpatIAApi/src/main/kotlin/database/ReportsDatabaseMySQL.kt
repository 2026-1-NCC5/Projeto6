package com.alimenpatia.database

import com.alimenpatia.models.*

object ReportsDatabaseMySQL {

    fun getAllMembers(role: String? = null, trend: String? = null): List<MemberDataDto> {
        val connection = DatabaseFactory.getConnection()

        var sql = """
            SELECT 
                u.id_usuario,
                u.nome,
                u.username,
                r.nome as role_name,
                u.criado_em as join_date,
                COALESCE(p.total_produtos, 0) as total_produtos,
                COALESCE(p.precisao, 0) as precisao,
                COALESCE(p.streak, 0) as streak,
                COALESCE(p.status_tendencia, 'estavel') as status_tendencia
            FROM usuarios u
            JOIN roles r ON u.id_role = r.id_role
            LEFT JOIN performance_usuario p ON u.id_usuario = p.id_usuario
            WHERE r.nome = 'Operador'
        """.trimIndent()

        if (!role.isNullOrBlank()) {
            sql += " AND r.nome = ?"
        }

        sql += " ORDER BY u.nome"

        val params = mutableListOf<Any>()
        if (!role.isNullOrBlank()) {
            params.add(role)
        }

        val members = mutableListOf<MemberDataDto>()

        try {
            val stmt = connection.prepareStatement(sql)
            params.forEachIndexed { index, value ->
                stmt.setString(index + 1, value.toString())
            }
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val trendStatus = rs.getString("status_tendencia")
                val mappedTrend = when (trendStatus) {
                    "em_alta" -> "UP"
                    "em_queda" -> "DOWN"
                    else -> "STABLE"
                }

                val precisao = rs.getInt("precisao")
                val totalProdutos = rs.getInt("total_produtos")
                val streak = rs.getInt("streak")

                val agilidade = (totalProdutos * 5).coerceAtMost(100)
                val organizacao = (streak * 10).coerceAtMost(100)

                members.add(
                    MemberDataDto(
                        id = "mbr_${String.format("%03d", rs.getInt("id_usuario"))}",
                        name = rs.getString("nome"),
                        role = "OPERADOR",
                        roleColor = "#1A73E8",
                        joinDate = rs.getString("join_date")?.take(10) ?: "2026-01-10",
                        trend = mappedTrend,
                        currentScore = precisao,
                        previousScore = (precisao - 4).coerceAtLeast(0),
                        scoreDelta = 4,
                        leaderNote = when (mappedTrend) {
                            "UP" -> "Operador em evolução positiva."
                            "DOWN" -> "Necessita acompanhamento mais próximo."
                            else -> "Mantém padrão consistente."
                        },
                        actionPlan = when (mappedTrend) {
                            "UP" -> "Continuar motivando e oferecer desafios."
                            "DOWN" -> "Revisar processos e oferecer treinamento."
                            else -> "Manter acompanhamento regular."
                        },
                        attributes = listOf(
                            AttributeDto("Precisao", precisao, (precisao - 4).coerceAtLeast(0), "#00E676"),
                            AttributeDto("Agilidade", agilidade, (agilidade - 5).coerceAtLeast(0), "#1A73E8"),
                            AttributeDto("Organizacao", organizacao, (organizacao - 3).coerceAtLeast(0), "#00BCD4"),
                            AttributeDto("Autonomia", 78, 72, "#7C4DFF")
                        ),
                        weeklyEvolution = listOf(72, 76, 78, 81, 84, precisao),
                        monthlyEvolution = listOf(68, 71, 74, 79, 83, precisao)
                    )
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        var filtered: List<MemberDataDto> = members
        if (!trend.isNullOrBlank()) {
            filtered = members.filter { it.trend.uppercase() == trend.uppercase() }
        }

        return filtered
    }

    fun getMemberById(id: String): MemberDataDto? {
        val memberId = id.replace("mbr_", "").toIntOrNull() ?: return null
        return getAllMembers().find { it.id == id }
    }

    fun getRoles(): List<String> {
        return listOf("OPERADOR")
    }

    fun calculateSummary(members: List<MemberDataDto>): ReportsSummary {
        if (members.isEmpty()) {
            return ReportsSummary(0, 0, 0, "")
        }

        val teamAttributeAverage = members.map { it.currentScore }.average().toInt()
        val averageDelta = members.map { it.scoreDelta }.average().toInt()
        val attentionCount = members.count { it.currentScore < 75 || it.trend == "DOWN" }
        val bestEvolutionMemberId = members.maxByOrNull { it.scoreDelta }?.id ?: ""

        return ReportsSummary(
            teamAttributeAverage = teamAttributeAverage,
            averageDelta = averageDelta,
            attentionCount = attentionCount,
            bestEvolutionMemberId = bestEvolutionMemberId
        )
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
                color = when (name.lowercase()) {
                    "precisao" -> "#00E676"
                    "agilidade" -> "#1A73E8"
                    "organizacao" -> "#00BCD4"
                    "autonomia" -> "#7C4DFF"
                    else -> "#1A73E8"
                }
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
        val members = getAllMembers()
        val totalProduction = members.sumOf { it.weeklyEvolution.sum() }
        val avgAccuracy = if (members.isNotEmpty()) {
            members.flatMap { it.attributes.map { attr -> attr.score } }.average().toInt()
        } else 0
        val maxStreak = if (members.isNotEmpty()) {
            members.flatMap { it.attributes.map { attr -> attr.score } }.maxOrNull() ?: 0
        } else 0
        val membersInUp = members.count { it.trend.uppercase() == "UP" }

        return ReportsStatsData(
            totalProduction = totalProduction,
            avgAccuracy = avgAccuracy,
            maxStreak = maxStreak,
            membersInUp = membersInUp,
            totalMembers = members.size
        )
    }
}