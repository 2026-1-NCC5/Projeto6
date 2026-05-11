package com.alimenpatia.routes

import com.alimenpatia.database.SessionDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sessionRoutes() {

    post("/sessions/start") {
        val request = try {
            call.receive<SessionRequest>()
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                SessionResponse(
                    success = false,
                    message = "Invalid request body: ${e.message}"
                )
            )
            return@post
        }

        when {
            request.qrCode.isBlank() -> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SessionResponse(
                        success = false,
                        message = "QR Code is required"
                    )
                )
                return@post
            }
            request.userId.isBlank() -> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SessionResponse(
                        success = false,
                        message = "User ID is required"
                    )
                )
                return@post
            }
            request.userName.isBlank() -> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SessionResponse(
                        success = false,
                        message = "User name is required"
                    )
                )
                return@post
            }
        }

        val response = SessionDatabaseMySQL.createSession(request)
        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
        call.respond(statusCode, response)
    }

    get("/sessions/active/{userId}") {
        val userId = call.parameters["userId"]
        if (userId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                SessionResponse(
                    success = false,
                    message = "User ID is required"
                )
            )
            return@get
        }

        val response = SessionDatabaseMySQL.getActiveSession(userId)
        call.respond(response)
    }

    post("/sessions/end") {
        val request = try {
            call.receive<SessionEndRequest>()
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                SessionEndResponse(
                    success = false,
                    message = "Invalid request body: ${e.message}"
                )
            )
            return@post
        }

        if (request.sessionId.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                SessionEndResponse(
                    success = false,
                    message = "Session ID is required"
                )
            )
            return@post
        }

        val response = SessionDatabaseMySQL.endSessionById(request)
        call.respond(response)
    }

    get("/sessions/history/{userId}") {
        val userId = call.parameters["userId"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

        if (userId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                UserSessionHistoryResponse(
                    success = false,
                    message = "User ID is required"
                )
            )
            return@get
        }

        val response = SessionDatabaseMySQL.getUserHistory(userId, limit)
        call.respond(response)
    }

    get("/sessions/stats/{userId}") {
        val userId = call.parameters["userId"]

        if (userId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "User ID is required")
            )
            return@get
        }

        val stats = SessionDatabaseMySQL.getSessionStats(userId)
        call.respond(mapOf("success" to true, "data" to stats))
    }
}