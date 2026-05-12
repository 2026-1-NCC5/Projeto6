package com.AlimempatIA.stockai.domain.auth

import com.AlimempatIA.stockai.domain.model.User
import com.AlimempatIA.stockai.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class ApiUser(
    val id: String,
    val name: String,
    val username: String,
    val role: String,
    val createdAt: String
)

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val success: Boolean, val user: ApiUser?, val token: String?, val message: String?)
data class CreateUserRequest(val name: String, val username: String, val password: String, val role: String)
data class UpdateUserRequest(val name: String? = null, val role: String? = null)
data class ResetPasswordRequest(val newPassword: String)

interface UserApiService {
    @GET("admin/users")
    suspend fun getUsers(): Map<String, Any>

    @POST("admin/users")
    suspend fun createUser(@Body request: CreateUserRequest): Map<String, Any>

    @PUT("admin/users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body request: UpdateUserRequest): Map<String, Any>

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Map<String, Any>

    @POST("admin/users/{id}/reset-password")
    suspend fun resetPassword(@Path("id") id: String, @Body request: ResetPasswordRequest): Map<String, Any>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val userService: UserApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApiService::class.java)
    }
}

object UserStore {

    private val usersCache = mutableListOf<User>()
    var currentUser: User? = null
        private set

    @Suppress("UNCHECKED_CAST")
    private fun parseUser(data: Map<String, Any>): User {
        return User(
            id = data["id"] as String,
            name = data["name"] as String,
            username = data["username"] as String,
            passwordHash = "",
            role = UserRole.valueOf(data["role"] as String)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserList(response: Map<String, Any>): List<User> {
        val data = response["users"] as? List<Map<String, Any>> ?: return emptyList()
        return data.map { parseUser(it) }
    }

    suspend fun register(
        name: String,
        username: String,
        password: String,
        role: UserRole = UserRole.OPERADOR
    ): RegisterResult {
        return withContext(Dispatchers.IO) {
            try {
                if (name.isBlank() || username.isBlank() || password.isBlank()) {
                    return@withContext RegisterResult.Error("Preencha todos os campos.")
                }
                if (username.length < 3) {
                    return@withContext RegisterResult.Error("Usuário deve ter ao menos 3 caracteres.")
                }
                if (password.length < 6) {
                    return@withContext RegisterResult.Error("Senha deve ter ao menos 6 caracteres.")
                }

                val response = ApiClient.userService.createUser(
                    CreateUserRequest(
                        name = name,
                        username = username,
                        password = password,
                        role = role.name
                    )
                )

                val success = response["success"] as? Boolean ?: false
                if (success) {
                    @Suppress("UNCHECKED_CAST")
                    val userData = response["data"] as? Map<String, Any>
                    if (userData != null) {
                        val user = parseUser(userData)
                        usersCache.add(user)
                        return@withContext RegisterResult.Success(user)
                    } else {
                        return@withContext RegisterResult.Error("Erro ao criar usuário")
                    }
                } else {
                    val message = response["message"] as? String ?: "Erro ao criar usuário"
                    return@withContext RegisterResult.Error(message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext RegisterResult.Error("Erro de rede: ${e.message}")
            }
        }
    }

    suspend fun login(username: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                if (username.isBlank() || password.isBlank()) {
                    return@withContext LoginResult.Error("Preencha todos os campos.")
                }

                val response = ApiClient.userService.login(LoginRequest(username, password))

                if (response.success && response.user != null) {
                    val user = User(
                        id = response.user.id,
                        name = response.user.name,
                        username = response.user.username,
                        passwordHash = "",
                        role = UserRole.valueOf(response.user.role)
                    )
                    currentUser = user
                    refreshCache()
                    return@withContext LoginResult.Success(user)
                } else {
                    return@withContext LoginResult.Error(response.message ?: "Usuário ou senha inválidos.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext LoginResult.Error("Erro de rede: ${e.message}")
            }
        }
    }

    private suspend fun refreshCache() {
        try {
            val response = ApiClient.userService.getUsers()
            usersCache.clear()
            usersCache.addAll(parseUserList(response))
            println("Cache atualizado: ${usersCache.size} usuários")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateName(userId: String, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (newName.isBlank()) return@withContext false

                val response = ApiClient.userService.updateUser(userId, UpdateUserRequest(name = newName))
                val success = response["success"] as? Boolean ?: false

                if (success) {
                    val idx = usersCache.indexOfFirst { it.id == userId }
                    if (idx != -1) {
                        usersCache[idx] = usersCache[idx].copy(name = newName)
                    }
                    if (currentUser?.id == userId) {
                        currentUser = currentUser?.copy(name = newName)
                    }
                }
                return@withContext success
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String): UpdatePasswordResult {
        return withContext(Dispatchers.IO) {
            try {
                if (oldPassword.isBlank() || newPassword.isBlank()) {
                    return@withContext UpdatePasswordResult.Error("Preencha todos os campos.")
                }
                if (newPassword.length < 6) {
                    return@withContext UpdatePasswordResult.Error("Nova senha deve ter ao menos 6 caracteres.")
                }

                val response = ApiClient.userService.resetPassword(userId, ResetPasswordRequest(newPassword))
                val success = response["success"] as? Boolean ?: false

                return@withContext if (success) {
                    UpdatePasswordResult.Success
                } else {
                    UpdatePasswordResult.Error(response["message"] as? String ?: "Erro ao atualizar senha")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext UpdatePasswordResult.Error("Erro de rede: ${e.message}")
            }
        }
    }

    fun logout() {
        currentUser = null
        usersCache.clear()
        println("Logout: cache limpo")
    }

    suspend fun getAllUsers(forceRefresh: Boolean = false): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (forceRefresh || usersCache.isEmpty()) {
                    refreshCache()
                }
                return@withContext usersCache.toList()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }
        }
    }

    suspend fun deleteUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (userId == currentUser?.id) return@withContext false

                val response = ApiClient.userService.deleteUser(userId)
                val success = response["success"] as? Boolean ?: false

                if (success) {
                    usersCache.removeIf { it.id == userId }
                }
                return@withContext success
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    suspend fun setRole(userId: String, role: UserRole) {
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.userService.updateUser(userId, UpdateUserRequest(role = role.name))
                val success = response["success"] as? Boolean ?: false

                if (success) {
                    val idx = usersCache.indexOfFirst { it.id == userId }
                    if (idx != -1) {
                        usersCache[idx] = usersCache[idx].copy(role = role)
                    }
                    if (currentUser?.id == userId) {
                        currentUser = currentUser?.copy(role = role)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun adminResetPassword(userId: String, newPassword: String) {
        withContext(Dispatchers.IO) {
            try {
                ApiClient.userService.resetPassword(userId, ResetPasswordRequest(newPassword))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

sealed class RegisterResult {
    data class Success(val user: User) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

sealed class UpdatePasswordResult {
    object Success : UpdatePasswordResult()
    data class Error(val message: String) : UpdatePasswordResult()
}