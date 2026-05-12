package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object DashboardDatabase {

    // 生成过去7天的数据
    fun generateWeeklyData(): WeeklyDataDto {
        val formatter = DateTimeFormatter.ofPattern("dd/MM")
        val weekDays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

        // 每日产品数量（用于柱状图）
        val dailyCounts = listOf(
            DailyCountDto("Seg", LocalDate.now().minusDays(6).format(formatter), 42),
            DailyCountDto("Ter", LocalDate.now().minusDays(5).format(formatter), 65),
            DailyCountDto("Qua", LocalDate.now().minusDays(4).format(formatter), 58),
            DailyCountDto("Qui", LocalDate.now().minusDays(3).format(formatter), 71),
            DailyCountDto("Sex", LocalDate.now().minusDays(2).format(formatter), 83),
            DailyCountDto("Sáb", LocalDate.now().minusDays(1).format(formatter), 76),
            DailyCountDto("Dom", LocalDate.now().format(formatter), 55)
        )

        // 累计数量（用于折线图）
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

    // 获取仪表盘统计数据
    fun getDashboardStats(): DashboardStatsData {
        val weeklyData = generateWeeklyData()
        val totalRecognized = weeklyData.cumulativeCounts.last().count
        val totalNotRecognized = 68  // 未识别数量

        return DashboardStatsData(
            recognized = totalRecognized,
            notRecognized = totalNotRecognized,
            totalTeam = totalRecognized + totalNotRecognized,
            weeklyData = weeklyData,
            goal = GoalInfoDto(
                current = totalRecognized,
                target = 600,
                percentage = (totalRecognized * 100 / 600)
            )
        )
    }

    // 获取识别统计
    fun getRecognitionStats(): RecognitionStatsDto {
        val stats = getDashboardStats()
        val total = stats.recognized + stats.notRecognized

        return RecognitionStatsDto(
            totalScans = total,
            recognizedCount = stats.recognized,
            notRecognizedCount = stats.notRecognized,
            recognitionRate = if (total > 0) stats.recognized.toFloat() / total * 100 else 0f,
            dailyAverage = stats.recognized / 7
        )
    }

    // 获取产品识别记录
    fun getProductRecognitions(limit: Int = 20): List<ProductRecognitionDto> {
        val products = listOf(
            "Arroz", "Feijão", "Macarrão", "Óleo", "Leite", "Café", "Açúcar",
            "Farinha", "Molho de Tomate", "Milho", "Ervilha", "Atum", "Sardinha",
            "Biscoito", "Refrigerante", "Suco", "Cerveja", "Vinho", "Sabão",
            "Detergente", "Arroz Integral", "Feijão Carioca", "Macarrão Integral",
            "Azeite", "Leite Sem Lactose", "Café Gourmet", "Açúcar Mascavo"
        )

        return (1..limit).map { i ->
            val recognized = Random.nextBoolean()
            ProductRecognitionDto(
                id = i.toString(),
                name = products.random(),
                wasRecognized = recognized,
                recognizedAt = if (recognized) java.time.Instant.now().toString() else null,
                confidence = if (recognized) Random.nextFloat() * 0.3f + 0.7f else null
            )
        }.sortedByDescending { it.recognizedAt }
    }
}