package com.alimenpatia.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val name: String,
    val username: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val role: UserRole? = null
)

@Serializable
data class ResetPasswordRequest(
    val newPassword: String
)

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