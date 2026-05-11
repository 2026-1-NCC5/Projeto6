package com.alimenpatia.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object DatabaseFactory {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = System.getenv("DB_URL") ?: "jdbc:mysql://localhost:3306/alimempatia_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"
        username = System.getenv("DB_USER") ?: "root"
        password = System.getenv("DB_PASSWORD") ?: ""
        driverClassName = "com.mysql.cj.jdbc.Driver"
        maximumPoolSize = (System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10)
        isAutoCommit = true
    }

    private val dataSource = HikariDataSource(hikariConfig)

    fun getConnection(): Connection {
        return dataSource.connection
    }

    fun close() {
        dataSource.close()
    }
}