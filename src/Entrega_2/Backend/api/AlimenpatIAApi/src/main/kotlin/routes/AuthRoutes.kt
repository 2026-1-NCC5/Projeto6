package com.alimenpatia.routes

import com.alimenpatia.database.UserDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {

    // POST /auth/login
    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val user = UserDatabaseMySQL.login(request.username, request.password)

        if (user != null) {
            call.respond(
                LoginResponse(
                    success = true,
                    user = user,
                    token = "jwt-token-${user.id}",
                    message = "Login successful"
                )
            )
        } else {
            call.respond(
                HttpStatusCode.Unauthorized,
                LoginResponse(
                    success = false,
                    message = "Usuário ou senha inválidos."
                )
            )
        }
    }

    // POST /auth/register
    post("/auth/register") {
        val request = call.receive<CreateUserRequest>()

        when {
            request.name.isBlank() -> {
                call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Nome é obrigatório"))
                return@post
            }
            request.username.isBlank() -> {
                call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Usuário é obrigatório"))
                return@post
            }
            request.password.length < 6 -> {
                call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Senha deve ter no mínimo 6 caracteres"))
                return@post
            }
        }

        val user = UserDatabaseMySQL.createUser(
            name = request.name,
            username = request.username,
            password = request.password,
            role = UserRole.OPERADOR
        )

        if (user != null) {
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    message = "Usuário registrado com sucesso",
                    data = mapOf(
                        "id" to user.id,
                        "name" to user.name,
                        "username" to user.username,
                        "role" to user.role.name
                    )
                )
            )
        } else {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.Conflict, ApiResponse(success = false, message = "Nome de usuário já existe"))
        }
    }
}