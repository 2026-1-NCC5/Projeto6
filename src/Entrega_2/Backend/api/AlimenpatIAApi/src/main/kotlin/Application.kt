package com.alimenpatia

import com.alimenpatia.routes.*
import com.alimenpatia.database.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    try {
        val connection = DatabaseFactory.getConnection()
        println("✅ Conexão com MySQL estabelecida com sucesso!")
        connection.close()
    } catch (e: Exception) {
        println("❌ Falha na conexão com MySQL: ${e.message}")
    }

    configureRouting()
}

fun Application.configureRouting() {
    routing {
        inventoryRoutes()
        productRoutes()
        userRoutes()
        authRoutes()
        dashboardRoutes()
        reportsRoutes()
        sessionRoutes()
        cameraRoutes()

    }
}