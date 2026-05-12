package com.alimenpatia.database

import com.alimenpatia.models.User
import com.alimenpatia.models.UserRole
import java.time.Instant
import java.util.UUID

object UserDatabase {
    private val users = mutableMapOf<String, StoredUser>()

    data class StoredUser(
        val id: String,
        val name: String,
        val username: String,
        val passwordHash: String,
        val role: UserRole,
        val createdAt: String
    )

    init {
        createUser(
            name = "Administrador",
            username = "admin",
            password = "admin123",
            role = UserRole.ADMINISTRADOR
        )

        createUser(
            name = "Carlos Mendes",
            username = "carlos.mendes",
            password = "123456",
            role = UserRole.OPERADOR
        )

        createUser(
            name = "Joao Silva",
            username = "joao.silva",
            password = "123456",
            role = UserRole.OPERADOR
        )
    }

    private fun hashPassword(password: String): String = password

    fun createUser(name: String, username: String, password: String, role: UserRole): User? {
        if (users.values.any { it.username == username }) return null

        val id = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        val user = StoredUser(
            id = id,
            name = name,
            username = username,
            passwordHash = hashPassword(password),
            role = role,
            createdAt = createdAt
        )
        users[id] = user

        return User(id, name, username, role, createdAt)
    }

    fun getAllUsers(): List<User> = users.values.map {
        User(it.id, it.name, it.username, it.role, it.createdAt)
    }

    fun getUserById(id: String): User? = users[id]?.let {
        User(it.id, it.name, it.username, it.role, it.createdAt)
    }

    fun getUserByUsername(username: String): User? = users.values.find {
        it.username == username
    }?.let {
        User(it.id, it.name, it.username, it.role, it.createdAt)
    }

    fun updateUser(id: String, name: String?, role: UserRole?): Boolean = users[id]?.let {
        users[id] = it.copy(
            name = name ?: it.name,
            role = role ?: it.role
        )
        true
    } ?: false

    fun deleteUser(id: String): Boolean = users.remove(id) != null

    fun resetPassword(id: String, newPassword: String): Boolean = users[id]?.let {
        users[id] = it.copy(passwordHash = hashPassword(newPassword))
        true
    } ?: false

    fun login(username: String, password: String): User? = users.values.find {
        it.username == username && it.passwordHash == hashPassword(password)
    }?.let {
        User(it.id, it.name, it.username, it.role, it.createdAt)
    }

    fun getUserStats(): Map<String, Int> = UserRole.entries.associate { role ->
        role.name to users.values.count { it.role == role }
    }
}