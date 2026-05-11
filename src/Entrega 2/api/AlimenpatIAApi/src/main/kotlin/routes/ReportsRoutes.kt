package com.alimenpatia.routes

import com.alimenpatia.database.ReportsDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reportsRoutes() {

    // GET /reports/operator-attributes
    get("/reports/operator-attributes") {
        val role = call.request.queryParameters["role"]
        val trend = call.request.queryParameters["trend"]
        val period = call.request.queryParameters["period"] ?: "weekly"

        val members = ReportsDatabaseMySQL.getAllMembers(role, trend)
        val summary = ReportsDatabaseMySQL.calculateSummary(members)
        val attributeMap = ReportsDatabaseMySQL.calculateAttributeMap(members)
        val trendCounts = ReportsDatabaseMySQL.calculateTrendCounts(members)

        val response = ReportsScreenResponse(
            summary = summary,
            attributeMap = attributeMap,
            trendCounts = trendCounts,
            members = members
        )
        call.respond(response)
    }

    // GET /reports/stats
    get("/reports/stats") {
        call.respond(ReportsStatsResponse(success = true, data = ReportsDatabaseMySQL.getReportsStats()))
    }

    // GET /reports/members
    get("/reports/members") {
        val role = call.request.queryParameters["role"]
        val trend = call.request.queryParameters["trend"]
        val members = ReportsDatabaseMySQL.getAllMembers(role, trend)
        call.respond(MemberListResponse(success = true, data = members, total = members.size))
    }

    // GET /reports/members/roles
    get("/reports/members/roles") {
        call.respond(RolesResponse(success = true, data = ReportsDatabaseMySQL.getRoles()))
    }

    // GET /reports/members/{id}
    get("/reports/members/{id}") {
        val id = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, MemberDetailResponse(false, message = "ID é obrigatório"))
            return@get
        }
        val member = ReportsDatabaseMySQL.getMemberById(id)
        if (member != null) {
            call.respond(MemberDetailResponse(success = true, data = member))
        } else {
            call.respond(HttpStatusCode.NotFound, MemberDetailResponse(false, message = "Membro não encontrado"))
        }
    }
}