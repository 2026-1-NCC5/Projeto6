package com.alimenpatia.database

import com.alimenpatia.models.User
import com.alimenpatia.models.UserRole

object UserDatabaseMySQL {

    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT u.id_usuario, u.nome, u.username, r.nome as role_name, u.criado_em
            FROM usuarios u
            JOIN roles r ON u.id_role = r.id_role
            ORDER BY u.id_usuario
        """.trimIndent()

        return try {
            val stmt = connection.prepareStatement(sql)
            val rs = stmt.executeQuery()

            println("=== Executando consulta SQL getAllUsers ===")

            while (rs.next()) {
                val roleNameFromDb = rs.getString("role_name")
                println("Role do DB: '$roleNameFromDb'")

                val roleName = when (roleNameFromDb) {
                    "Administrador" -> UserRole.ADMINISTRADOR
                    "Coordenação" -> UserRole.COORDENACAO
                    "Supervisão" -> UserRole.SUPERVISAO
                    "Conselho de Mentoria" -> UserRole.CONSELHO_MENTORIA
                    "Operador" -> UserRole.OPERADOR
                    else -> {
                        println("Role desconhecida: $roleNameFromDb, definindo como OPERADOR")
                        UserRole.OPERADOR
                    }
                }

                users.add(
                    User(
                        id = "usr_${String.format("%03d", rs.getInt("id_usuario"))}",
                        name = rs.getString("nome"),
                        username = rs.getString("username"),
                        role = roleName,
                        createdAt = rs.getString("criado_em")
                    )
                )
            }
            println("Total de usuários encontrados: ${users.size}")
            connection.close()
            users
        } catch (e: Exception) {
            println("Erro na consulta getAllUsers: ${e.message}")
            e.printStackTrace()
            connection.close()
            emptyList()
        }
    }

    fun getUserById(id: String): User? {
        val userId = id.replace("usr_", "").toIntOrNull() ?: return null
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT u.id_usuario, u.nome, u.username, r.nome as role_name, u.criado_em
            FROM usuarios u
            JOIN roles r ON u.id_role = r.id_role
            WHERE u.id_usuario = ?
        """.trimIndent()

        return try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, userId)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val roleNameFromDb = rs.getString("role_name")
                val roleName = when (roleNameFromDb) {
                    "Administrador" -> UserRole.ADMINISTRADOR
                    "Coordenação" -> UserRole.COORDENACAO
                    "Supervisão" -> UserRole.SUPERVISAO
                    "Conselho de Mentoria" -> UserRole.CONSELHO_MENTORIA
                    else -> UserRole.OPERADOR
                }
                val user = User(
                    id = "usr_${String.format("%03d", rs.getInt("id_usuario"))}",
                    name = rs.getString("nome"),
                    username = rs.getString("username"),
                    role = roleName,
                    createdAt = rs.getString("criado_em")
                )
                connection.close()
                user
            } else {
                connection.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            null
        }
    }

    fun getUserByUsername(username: String): User? {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT u.id_usuario, u.nome, u.username, r.nome as role_name, u.criado_em
            FROM usuarios u
            JOIN roles r ON u.id_role = r.id_role
            WHERE u.username = ?
        """.trimIndent()

        return try {
            val stmt = connection.prepareStatement(sql)
            stmt.setString(1, username)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val roleNameFromDb = rs.getString("role_name")
                val roleName = when (roleNameFromDb) {
                    "Administrador" -> UserRole.ADMINISTRADOR
                    "Coordenação" -> UserRole.COORDENACAO
                    "Supervisão" -> UserRole.SUPERVISAO
                    "Conselho de Mentoria" -> UserRole.CONSELHO_MENTORIA
                    else -> UserRole.OPERADOR
                }
                val user = User(
                    id = "usr_${String.format("%03d", rs.getInt("id_usuario"))}",
                    name = rs.getString("nome"),
                    username = rs.getString("username"),
                    role = roleName,
                    createdAt = rs.getString("criado_em")
                )
                connection.close()
                user
            } else {
                connection.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            null
        }
    }

    fun login(username: String, password: String): User? {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT u.id_usuario, u.nome, u.username, r.nome as role_name, u.criado_em
            FROM usuarios u
            JOIN roles r ON u.id_role = r.id_role
            WHERE u.username = ? AND u.senha_hash = ?
        """.trimIndent()

        return try {
            val stmt = connection.prepareStatement(sql)
            stmt.setString(1, username)
            stmt.setString(2, password)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val roleNameFromDb = rs.getString("role_name")
                val roleName = when (roleNameFromDb) {
                    "Administrador" -> UserRole.ADMINISTRADOR
                    "Coordenação" -> UserRole.COORDENACAO
                    "Supervisão" -> UserRole.SUPERVISAO
                    "Conselho de Mentoria" -> UserRole.CONSELHO_MENTORIA
                    else -> UserRole.OPERADOR
                }
                val user = User(
                    id = "usr_${String.format("%03d", rs.getInt("id_usuario"))}",
                    name = rs.getString("nome"),
                    username = rs.getString("username"),
                    role = roleName,
                    createdAt = rs.getString("criado_em")
                )
                connection.close()
                user
            } else {
                connection.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            null
        }
    }

    fun updateUser(id: String, name: String?, role: UserRole?): Boolean {
        val userId = id.replace("usr_", "").toIntOrNull() ?: return false
        val connection = DatabaseFactory.getConnection()

        return try {
            if (name != null) {
                val stmt = connection.prepareStatement("UPDATE usuarios SET nome = ? WHERE id_usuario = ?")
                stmt.setString(1, name)
                stmt.setInt(2, userId)
                stmt.executeUpdate()
            }
            if (role != null) {
                val roleId = when (role) {
                    UserRole.ADMINISTRADOR -> 5
                    UserRole.COORDENACAO -> 4
                    UserRole.SUPERVISAO -> 2
                    UserRole.CONSELHO_MENTORIA -> 3
                    else -> 1
                }
                val stmt = connection.prepareStatement("UPDATE usuarios SET id_role = ? WHERE id_usuario = ?")
                stmt.setInt(1, roleId)
                stmt.setInt(2, userId)
                stmt.executeUpdate()
            }
            connection.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            false
        }
    }

    fun resetPassword(id: String, newPassword: String): Boolean {
        val userId = id.replace("usr_", "").toIntOrNull() ?: return false
        val connection = DatabaseFactory.getConnection()

        return try {
            val stmt = connection.prepareStatement("UPDATE usuarios SET senha_hash = ? WHERE id_usuario = ?")
            stmt.setString(1, newPassword)
            stmt.setInt(2, userId)
            val rows = stmt.executeUpdate()
            connection.close()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            false
        }
    }

    fun deleteUser(id: String): Boolean {
        val userId = id.replace("usr_", "").toIntOrNull() ?: return false
        val connection = DatabaseFactory.getConnection()

        return try {
            val stmt = connection.prepareStatement("DELETE FROM usuarios WHERE id_usuario = ?")
            stmt.setInt(1, userId)
            val rows = stmt.executeUpdate()
            connection.close()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            false
        }
    }

    fun createUser(name: String, username: String, password: String, role: UserRole): User? {
        val connection = DatabaseFactory.getConnection()
        val roleId = when (role) {
            UserRole.ADMINISTRADOR -> 5
            UserRole.COORDENACAO -> 4
            UserRole.SUPERVISAO -> 2
            UserRole.CONSELHO_MENTORIA -> 3
            else -> 1
        }
        val sql = "INSERT INTO usuarios (nome, username, senha_hash, id_role) VALUES (?, ?, ?, ?)"

        return try {
            val stmt = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            stmt.setString(1, name)
            stmt.setString(2, username)
            stmt.setString(3, password)
            stmt.setInt(4, roleId)
            val affected = stmt.executeUpdate()

            if (affected > 0) {
                val rs = stmt.generatedKeys
                if (rs.next()) {
                    val newId = rs.getInt(1)
                    connection.close()
                    return getUserById("usr_${String.format("%03d", newId)}")
                }
            }
            connection.close()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            null
        }
    }

    fun getUserStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT r.nome as role_name, COUNT(u.id_usuario) as count
            FROM usuarios u
            JOIN roles r ON u.id_role = r.id_role
            GROUP BY r.nome
        """.trimIndent()

        return try {
            val stmt = connection.prepareStatement(sql)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val roleNameFromDb = rs.getString("role_name")
                val count = rs.getInt("count")
                val key = when (roleNameFromDb) {
                    "Administrador" -> "ADMINISTRADOR"
                    "Coordenação" -> "COORDENACAO"
                    "Supervisão" -> "SUPERVISAO"
                    "Conselho de Mentoria" -> "CONSELHO_MENTORIA"
                    else -> "OPERADOR"
                }
                stats[key] = count
            }
            connection.close()
            UserRole.entries.forEach { role ->
                if (!stats.containsKey(role.name)) stats[role.name] = 0
            }
            stats
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            emptyMap()
        }
    }
}