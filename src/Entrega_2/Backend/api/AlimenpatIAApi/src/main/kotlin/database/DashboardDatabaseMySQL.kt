package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DashboardDatabaseMySQL {

    fun generateWeeklyData(equipeId: Int = 1): WeeklyDataDto {
        val connection = DatabaseFactory.getConnection()
        val weekDays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

        val today = LocalDate.now()
        val dates = (0..6).map { today.minusDays(6 - it.toLong()) }

        val dailyCounts = mutableListOf<DailyCountDto>()

        for (i in dates.indices) {
            val date = dates[i]
            val dayOfWeek = date.dayOfWeek.value
            val weekDay = when (dayOfWeek) {
                1 -> "Seg"
                2 -> "Ter"
                3 -> "Qua"
                4 -> "Qui"
                5 -> "Sex"
                6 -> "Sáb"
                7 -> "Dom"
                else -> "Seg"
            }

            val formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM"))

            val sql = """
                SELECT COUNT(*) as count
                FROM registros r
                JOIN produtos p ON r.id_produto = p.id_produto
                WHERE DATE(r.data_registro) = ?
                AND r.id_equipe = ?
            """.trimIndent()

            val stmt = connection.prepareStatement(sql)
            stmt.setString(1, date.toString())
            stmt.setInt(2, equipeId)
            val rs = stmt.executeQuery()

            var count = 0
            if (rs.next()) {
                count = rs.getInt("count")
            }
            rs.close()
            stmt.close()

            dailyCounts.add(DailyCountDto(weekDay, formattedDate, count))
        }

        connection.close()

        var cumulative = 0
        val cumulativeCounts = dailyCounts.map { daily ->
            cumulative += daily.count
            DailyCountDto(daily.day, daily.date, cumulative)
        }

        return WeeklyDataDto(
            dailyCounts = dailyCounts,
            cumulativeCounts = cumulativeCounts,
            weekDays = weekDays
        )
    }

    fun getDashboardStats(equipeId: Int = 1): DashboardStatsData {
        println("=== DashboardDatabaseMySQL.getDashboardStats() chamado com equipeId: $equipeId ===")

        val connection = DatabaseFactory.getConnection()

        val recognizedSql = """
            SELECT COUNT(*) as count
            FROM registros r
            WHERE r.id_equipe = ?
        """.trimIndent()

        var recognized = 0
        val stmt1 = connection.prepareStatement(recognizedSql)
        stmt1.setInt(1, equipeId)
        val rs1 = stmt1.executeQuery()
        if (rs1.next()) {
            recognized = rs1.getInt("count")
        }
        println("=== Reconhecidos (registros): $recognized ===")
        rs1.close()
        stmt1.close()

        val notRecognizedSql = """
            SELECT COUNT(*) as count
            FROM deteccoes d
            JOIN sessoes s ON d.id_sessao = s.id_sessao
            WHERE s.id_equipe = ? AND d.reconhecido = 0
        """.trimIndent()

        var notRecognized = 0
        val stmt2 = connection.prepareStatement(notRecognizedSql)
        stmt2.setInt(1, equipeId)
        val rs2 = stmt2.executeQuery()
        if (rs2.next()) {
            notRecognized = rs2.getInt("count")
        }
        println("=== Não reconhecidos (deteccoes where reconhecido=0): $notRecognized ===")
        rs2.close()
        stmt2.close()

        val totalTeam = recognized + notRecognized
        val weeklyData = generateWeeklyData(equipeId)
        val current = weeklyData.cumulativeCounts.lastOrNull()?.count ?: totalTeam
        val target = 600
        val percentage = if (target > 0) (current * 100 / target) else 0

        connection.close()

        println("=== Estatísticas finais: reconhecidos=$recognized, nãoReconhecidos=$notRecognized, totalEquipe=$totalTeam ===")

        return DashboardStatsData(
            recognized = recognized,
            notRecognized = notRecognized,
            totalTeam = totalTeam,
            weeklyData = weeklyData,
            goal = GoalInfoDto(
                current = current,
                target = target,
                percentage = percentage
            )
        )
    }

    fun getRecognitionStats(equipeId: Int = 1): RecognitionStatsDto {
        val stats = getDashboardStats(equipeId)
        val total = stats.recognized + stats.notRecognized

        return RecognitionStatsDto(
            totalScans = total,
            recognizedCount = stats.recognized,
            notRecognizedCount = stats.notRecognized,
            recognitionRate = if (total > 0) stats.recognized.toFloat() / total * 100 else 0f,
            dailyAverage = if (stats.recognized > 0) stats.recognized / 7 else 0
        )
    }

    fun getProductRecognitions(limit: Int = 20, equipeId: Int = 1): List<ProductRecognitionDto> {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT 
                r.id_registro,
                p.nome as product_name,
                r.data_registro
            FROM registros r
            JOIN produtos p ON r.id_produto = p.id_produto
            WHERE r.id_equipe = ?
            ORDER BY r.data_registro DESC
            LIMIT ?
        """.trimIndent()

        val recognitions = mutableListOf<ProductRecognitionDto>()

        try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, equipeId)
            stmt.setInt(2, limit)
            val rs = stmt.executeQuery()

            var index = 1
            while (rs.next()) {
                recognitions.add(
                    ProductRecognitionDto(
                        id = index.toString(),
                        name = rs.getString("product_name"),
                        wasRecognized = true,
                        recognizedAt = rs.getString("data_registro"),
                        confidence = 98.5f
                    )
                )
                index++
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return recognitions
    }
}