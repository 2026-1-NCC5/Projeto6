package com.alimenpatia.routes

import com.alimenpatia.database.UserDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {

    fun getCurrentUserId(call: ApplicationCall): String {
        val adminUser = UserDatabaseMySQL.getUserByUsername("admin")
        return adminUser?.id ?: "usr_001"
    }

    // GET /admin/users
    get("/admin/users") {
        val currentUserId = getCurrentUserId(call)
        val users = UserDatabaseMySQL.getAllUsers()
        val stats = UserDatabaseMySQL.getUserStats()
        val currentUser = UserDatabaseMySQL.getUserById(currentUserId)

        val response = AdminUsersResponse(
            currentUser = currentUser ?: User("", "", "", UserRole.OPERADOR),
            stats = UserStats(
                totalUsers = users.size,
                countByRole = stats
            ),
            availableRoles = UserRole.entries.map { it.name },
            creatableRoles = listOf("SUPERVISAO", "CONSELHO_MENTORIA", "COORDENACAO", "ADMINISTRADOR"),
            users = users.map { user ->
                AdminUser(
                    id = user.id,
                    name = user.name,
                    username = user.username,
                    role = user.role.name,
                    isSelf = user.id == currentUserId
                )
            }
        )
        call.respond(response)
    }

    // POST /admin/users
    post("/admin/users") {
        try {
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

            val role = try {
                UserRole.valueOf(request.role.uppercase())
            } catch (e: IllegalArgumentException) {
                call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Perfil de acesso inválido: ${request.role}"))
                return@post
            }

            val user = UserDatabaseMySQL.createUser(
                name = request.name,
                username = request.username,
                password = request.password,
                role = role
            )

            if (user != null) {
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(
                        success = true,
                        message = "Usuário criado com sucesso",
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
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = "Erro interno: ${e.message}"))
        }
    }

    // PUT /admin/users/{id}
    put("/admin/users/{id}") {
        val id = call.parameters["id"]
        if (id == null) {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "ID é obrigatório"))
            return@put
        }

        val request = call.receive<UpdateUserRequest>()
        val role = request.role?.let { roleName ->
            try {
                UserRole.valueOf(roleName.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        val updated = UserDatabaseMySQL.updateUser(id, request.name, role)

        if (updated) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = true, message = "Usuário atualizado"))
        } else {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.NotFound, ApiResponse(success = false, message = "Usuário não encontrado"))
        }
    }

    // DELETE /admin/users/{id}
    delete("/admin/users/{id}") {
        val id = call.parameters["id"]
        if (id == null) {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "ID é obrigatório"))
            return@delete
        }

        val currentUserId = getCurrentUserId(call)
        if (id == currentUserId) {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Não é possível excluir o próprio usuário"))
            return@delete
        }

        val deleted = UserDatabaseMySQL.deleteUser(id)
        if (deleted) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = true, message = "Usuário excluído"))
        } else {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.NotFound, ApiResponse(success = false, message = "Usuário não encontrado"))
        }
    }

    // POST /admin/users/{id}/reset-password
    post("/admin/users/{id}/reset-password") {
        val id = call.parameters["id"]
        if (id == null) {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "ID é obrigatório"))
            return@post
        }

        val request = call.receive<ResetPasswordRequest>()
        if (request.newPassword.length < 6) {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Senha deve ter no mínimo 6 caracteres"))
            return@post
        }

        val reset = UserDatabaseMySQL.resetPassword(id, request.newPassword)
        if (reset) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = true, message = "Senha redefinida com sucesso"))
        } else {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.NotFound, ApiResponse(success = false, message = "Usuário não encontrado"))
        }
    }


    // GET /me/settings
    get("/me/settings") {
        val currentUserId = getCurrentUserId(call)
        val currentUser = UserDatabaseMySQL.getUserById(currentUserId)

        if (currentUser == null) {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.Unauthorized, ApiResponse(success = false, message = "Usuário não encontrado"))
            return@get
        }

        val rolesHierarchy = listOf("OPERADOR", "SUPERVISAO", "CONSELHO_MENTORIA", "COORDENACAO", "ADMINISTRADOR")
        val permissions = UserPermissions(
            canAccessAdminPanel = currentUser.role == UserRole.ADMINISTRADOR,
            canEditOwnName = true,
            canChangePassword = true
        )

        val response = UserSettings(
            user = currentUser,
            rolesHierarchy = rolesHierarchy,
            permissions = permissions
        )
        call.respond(response)
    }

    // PUT /me/name
    put("/me/name") {
        val currentUserId = getCurrentUserId(call)
        val request = call.receive<UpdateNameRequest>()

        if (request.name.isBlank()) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = false, message = "Nome não pode estar vazio"))
            return@put
        }

        val updated = UserDatabaseMySQL.updateUser(currentUserId, request.name, null)
        if (updated) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = true, message = "Nome atualizado com sucesso"))
        } else {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = false, message = "Erro ao atualizar nome"))
        }
    }

    // POST /me/change-password
    post("/me/change-password") {
        val currentUserId = getCurrentUserId(call)
        val request = call.receive<ChangePasswordRequest>()

        val currentUser = UserDatabaseMySQL.getUserById(currentUserId) ?: run {
            call.respond<ApiResponse<Nothing>>(HttpStatusCode.Unauthorized, ApiResponse(success = false, message = "Usuário não encontrado"))
            return@post
        }

        val loginCheck = UserDatabaseMySQL.login(currentUser.username, request.oldPassword)
        if (loginCheck == null) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = false, message = "Senha atual incorreta"))
            return@post
        }

        when {
            request.newPassword.length < 6 -> {
                call.respond<ApiResponse<Nothing>>(ApiResponse(success = false, message = "Nova senha deve ter no mínimo 6 caracteres"))
                return@post
            }
            request.newPassword != request.confirmPassword -> {
                call.respond<ApiResponse<Nothing>>(ApiResponse(success = false, message = "As senhas não coincidem"))
                return@post
            }
        }

        val reset = UserDatabaseMySQL.resetPassword(currentUserId, request.newPassword)
        if (reset) {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = true, message = "Senha alterada com sucesso"))
        } else {
            call.respond<ApiResponse<Nothing>>(ApiResponse(success = false, message = "Erro ao alterar senha"))
        }
    }
}