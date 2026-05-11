package com.alimenpatia.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class User(
    val id: String,
    val name: String,
    val username: String,
    val role: UserRole,
    val createdAt: String = Instant.now().toString()
)

@Serializable
enum class UserRole(val label: String) {
    OPERADOR("Operador"),
    SUPERVISAO("Supervisão"),
    CONSELHO_MENTORIA("Conselho/Mentoria"),
    COORDENACAO("Coordenação"),
    ADMINISTRADOR("Administrador")
}

@Serializable
data class AdminUser(
    val id: String,
    val name: String,
    val username: String,
    val role: String,
    val isSelf: Boolean
)

@Serializable
data class AdminUsersResponse(
    val currentUser: User,
    val stats: UserStats,
    val availableRoles: List<String>,
    val creatableRoles: List<String>,
    val users: List<AdminUser>
)

@Serializable
data class UserStats(
    val totalUsers: Int,
    val countByRole: Map<String, Int>
)

@Serializable
data class UserSettings(
    val user: User,
    val rolesHierarchy: List<String>,
    val permissions: UserPermissions
)

@Serializable
data class UserPermissions(
    val canAccessAdminPanel: Boolean,
    val canEditOwnName: Boolean,
    val canChangePassword: Boolean
)

// Request models
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val user: User? = null,
    val token: String? = null,
    val message: String? = null
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val username: String,
    val password: String,
    val role: String
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val role: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val newPassword: String
)

@Serializable
data class UpdateNameRequest(
    val name: String
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
)