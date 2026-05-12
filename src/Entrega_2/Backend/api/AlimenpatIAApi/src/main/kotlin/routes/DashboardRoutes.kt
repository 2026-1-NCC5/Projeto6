package com.alimenpatia.routes

import com.alimenpatia.database.DashboardDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dashboardRoutes() {

    // GET /dashboard/stats
    get("/dashboard/stats") {
        val equipeId = call.request.queryParameters["equipeId"]?.toIntOrNull() ?: 1
        println("=== Dashboard API called with equipeId: $equipeId ===")
        val stats = DashboardDatabaseMySQL.getDashboardStats(equipeId)
        call.respond(
            DashboardStatsResponse(
                success = true,
                data = stats
            )
        )
    }

    // GET /dashboard/weekly
    get("/dashboard/weekly") {
        val equipeId = call.request.queryParameters["equipeId"]?.toIntOrNull() ?: 1
        val weeklyData = DashboardDatabaseMySQL.generateWeeklyData(equipeId)
        call.respond(
            WeeklyDataResponse(
                success = true,
                data = weeklyData
            )
        )
    }

    // GET /dashboard/goal
    get("/dashboard/goal") {
        val equipeId = call.request.queryParameters["equipeId"]?.toIntOrNull() ?: 1
        val stats = DashboardDatabaseMySQL.getDashboardStats(equipeId)
        call.respond(
            GoalProgressResponse(
                success = true,
                data = stats.goal
            )
        )
    }

    // GET /dashboard/recognitions
    get("/dashboard/recognitions") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val equipeId = call.request.queryParameters["equipeId"]?.toIntOrNull() ?: 1
        val recognitions = DashboardDatabaseMySQL.getProductRecognitions(limit, equipeId)
        call.respond(
            ProductRecognitionResponse(
                success = true,
                data = recognitions,
                total = recognitions.size
            )
        )
    }

    // GET /dashboard/recognition-stats
    get("/dashboard/recognition-stats") {
        val equipeId = call.request.queryParameters["equipeId"]?.toIntOrNull() ?: 1
        val stats = DashboardDatabaseMySQL.getRecognitionStats(equipeId)
        call.respond(
            RecognitionStatsResponse(
                success = true,
                data = stats
            )
        )
    }
}