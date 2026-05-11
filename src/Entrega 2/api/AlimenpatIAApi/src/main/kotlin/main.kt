package com.alimenpatia

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty

fun main() {
    println("Starting server...")

    embeddedServer(Netty, port = 8080) {

        module()
    }.start(wait = true)

    println("Server started on port 8080")
}