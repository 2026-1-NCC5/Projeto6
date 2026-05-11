package com.AlimempatIA.stockai.domain.model

enum class UserRole(val label: String) {
    OPERADOR("Operador"),
    SUPERVISAO("Supervisão"),
    CONSELHO_MENTORIA("Conselho de Mentoria"),
    COORDENACAO("Coordenação"),
    ADMINISTRADOR("Administrador")
}

data class User(
    val id: String,
    val name: String,
    val username: String,
    val passwordHash: String,
    val role: UserRole = UserRole.OPERADOR
)
