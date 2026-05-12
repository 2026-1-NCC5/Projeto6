package com.alimenpatia.routes

import com.alimenpatia.database.CameraDatabaseMySQL
import com.alimenpatia.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cameraRoutes() {

    // POST /camera/scans
    post("/camera/scans") {
        val request = try {
            call.receive<QRScanRequest>()
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                QRScanResponse(
                    success = false,
                    message = "Invalid request body"
                )
            )
            return@post
        }

        if (request.qrCode.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                QRScanResponse(
                    success = false,
                    message = "QR Code is required"
                )
            )
            return@post
        }

        val result = CameraDatabaseMySQL.processScan(request.qrCode)
        call.respond(result)
    }

    // GET /camera/machines
    get("/camera/machines") {
        val machines = CameraDatabaseMySQL.getAllMachines()
        call.respond(
            CameraMachinesResponse(
                success = true,
                data = machines,
                total = machines.size
            )
        )
    }

    // GET /camera/machines/{code}
    get("/camera/machines/{code}") {
        val code = call.parameters["code"]
        if (code == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                QRScanResponse(
                    success = false,
                    message = "QR Code is required"
                )
            )
            return@get
        }

        val machine = CameraDatabaseMySQL.getMachineByQRCode(code)
        if (machine != null) {
            call.respond(
                QRScanResponse(
                    success = true,
                    data = machine
                )
            )
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                QRScanResponse(
                    success = false,
                    message = "Machine not found for QR Code: $code"
                )
            )
        }
    }

    // GET /camera/machines/{id}/stock
    get("/camera/machines/{id}/stock") {
        val id = call.parameters["id"]
        if (id == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                MachineStockResponse(
                    success = false,
                    message = "Machine ID is required"
                )
            )
            return@get
        }

        val stock = CameraDatabaseMySQL.getMachineStock(id)
        if (stock != null) {
            call.respond(
                MachineStockResponse(
                    success = true,
                    data = stock
                )
            )
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                MachineStockResponse(
                    success = false,
                    message = "Machine not found"
                )
            )
        }
    }

    // GET /camera/history
    get("/camera/history") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val history = CameraDatabaseMySQL.getScanHistory(limit)
        call.respond(
            ScanHistoryResponse(
                success = true,
                data = history,
                total = history.size
            )
        )
    }

    // GET /camera/stats
    get("/camera/stats") {
        val stats = CameraDatabaseMySQL.getScanStats()
        call.respond(
            ApiResponse(
                success = true,
                data = stats
            )
        )
    }
}