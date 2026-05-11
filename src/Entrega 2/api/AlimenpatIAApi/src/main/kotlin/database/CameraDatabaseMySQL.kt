package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.Instant

object CameraDatabaseMySQL {

    fun getAllMachines(): List<MachineInfo> {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT 
                id_equipe as id,
                nome as name,
                nome as code,
                'Área de Produção' as location,
                'ONLINE' as status
            FROM equipes
            ORDER BY id_equipe
        """.trimIndent()

        val machines = mutableListOf<MachineInfo>()

        try {
            val stmt = connection.prepareStatement(sql)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                machines.add(
                    MachineInfo(
                        id = "mac_${String.format("%03d", rs.getInt("id"))}",
                        name = rs.getString("name"),
                        code = rs.getString("code"),
                        location = rs.getString("location"),
                        status = rs.getString("status"),
                        lastScan = null,
                        productCount = 0,
                        products = emptyList()
                    )
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return machines
    }

    fun getMachineByQRCode(qrCode: String): MachineInfo? {
        val machineName = if (qrCode.startsWith("MACHINE:")) {
            qrCode.substringAfter("MACHINE:")
        } else {
            qrCode
        }

        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT id_equipe, nome
            FROM equipes
            WHERE nome LIKE ?
        """.trimIndent()

        try {
            val stmt = connection.prepareStatement(sql)
            stmt.setString(1, "%$machineName%")
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val machine = MachineInfo(
                    id = "mac_${String.format("%03d", rs.getInt("id_equipe"))}",
                    name = rs.getString("nome"),
                    code = rs.getString("nome"),
                    location = "Área de Produção",
                    status = "ONLINE",
                    lastScan = null,
                    productCount = 0,
                    products = emptyList()
                )
                rs.close()
                stmt.close()
                connection.close()
                return machine
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return null
    }

    fun getMachineById(id: String): MachineInfo? {
        val machineId = id.replace("mac_", "").toIntOrNull() ?: return null

        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT id_equipe, nome
            FROM equipes
            WHERE id_equipe = ?
        """.trimIndent()

        try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, machineId)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val machine = MachineInfo(
                    id = "mac_${String.format("%03d", rs.getInt("id_equipe"))}",
                    name = rs.getString("nome"),
                    code = rs.getString("nome"),
                    location = "Área de Produção",
                    status = "ONLINE",
                    lastScan = null,
                    productCount = 0,
                    products = emptyList()
                )
                rs.close()
                stmt.close()
                connection.close()
                return machine
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return null
    }

    fun getMachineStock(machineId: String): MachineStock? {
        val machine = getMachineById(machineId) ?: return null

        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT 
                p.id_produto,
                p.nome,
                p.sku,
                p.quantidade,
                COALESCE(c.nome, 'OUTROS') as categoria
            FROM produtos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LIMIT 10
        """.trimIndent()

        val products = mutableListOf<ProductStockDetail>()

        try {
            val stmt = connection.prepareStatement(sql)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val quantity = rs.getInt("quantidade")
                val minStock = 10
                val status = when {
                    quantity == 0 -> "OUT_OF_STOCK"
                    quantity <= minStock -> "LOW_STOCK"
                    else -> "IN_STOCK"
                }

                products.add(
                    ProductStockDetail(
                        id = "prd_${String.format("%03d", rs.getInt("id_produto"))}",
                        name = rs.getString("nome"),
                        sku = rs.getString("sku"),
                        quantity = quantity,
                        minStock = minStock,
                        status = status
                    )
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        val inStockCount = products.count { it.status == "IN_STOCK" }
        val lowStockCount = products.count { it.status == "LOW_STOCK" }
        val outOfStockCount = products.count { it.status == "OUT_OF_STOCK" }

        return MachineStock(
            machineId = machine.id,
            machineName = machine.name,
            machineLocation = machine.location,
            totalProducts = products.size,
            inStockCount = inStockCount,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            products = products
        )
    }

    fun getScanStats(): Map<String, Any> {
        val connection = DatabaseFactory.getConnection()

        val totalSql = "SELECT COUNT(*) as total FROM deteccoes"
        var totalScans = 0
        var successScans = 0

        try {
            val stmt = connection.prepareStatement(totalSql)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                totalScans = rs.getInt("total")
            }
            rs.close()
            stmt.close()

            val successSql = "SELECT COUNT(*) as total FROM deteccoes WHERE reconhecido = 1"
            val stmt2 = connection.prepareStatement(successSql)
            val rs2 = stmt2.executeQuery()
            if (rs2.next()) {
                successScans = rs2.getInt("total")
            }
            rs2.close()
            stmt2.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        val successRate = if (totalScans > 0) {
            successScans.toFloat() / totalScans * 100
        } else {
            0f
        }

        return mapOf(
            "totalScans" to totalScans,
            "successScans" to successScans,
            "successRate" to successRate,
            "uniqueMachines" to 1,
            "lastScan" to Instant.now().toString()
        )
    }

    fun processScan(qrCode: String, userId: String? = null): QRScanResponse {
        val machine = getMachineByQRCode(qrCode)

        if (machine == null) {
            return QRScanResponse(
                success = false,
                message = "QR Code inválido. Máquina não encontrada."
            )
        }

        val connection = DatabaseFactory.getConnection()
        val sql = """
            INSERT INTO deteccoes (id_sessao, id_produto, classe_detectada, reconhecido, detectado_em)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        try {
            val sessionId = getOrCreateActiveSession(userId)

            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, sessionId)
            stmt.setNull(2, java.sql.Types.INTEGER)
            stmt.setString(3, "outros")
            stmt.setBoolean(4, true)
            stmt.setString(5, Instant.now().toString())
            stmt.executeUpdate()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return QRScanResponse(
            success = true,
            data = machine,
            message = "QR Code válido! Máquina: ${machine.name}"
        )
    }

    private fun getOrCreateActiveSession(userId: String?): Int {
        val connection = DatabaseFactory.getConnection()

        val findSql = """
            SELECT id_sessao FROM sessoes 
            WHERE status = 'ativa' 
            ORDER BY data_inicio DESC LIMIT 1
        """.trimIndent()

        try {
            val stmt = connection.prepareStatement(findSql)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val sessionId = rs.getInt("id_sessao")
                rs.close()
                stmt.close()
                connection.close()
                return sessionId
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val insertSql = """
            INSERT INTO sessoes (id_usuario, id_equipe, data_inicio, status)
            VALUES (?, 1, NOW(), 'ativa')
        """.trimIndent()

        try {
            val stmt = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)
            val userIdInt = userId?.toIntOrNull() ?: 1
            stmt.setInt(1, userIdInt)
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            if (rs.next()) {
                val newSessionId = rs.getInt(1)
                rs.close()
                stmt.close()
                connection.close()
                return newSessionId
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return 1
    }

    fun getScanHistory(limit: Int = 20): List<ScanRecord> {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT 
                d.id_deteccao as id,
                d.classe_detectada as qr_code,
                d.reconhecido as result,
                d.detectado_em as scanned_at,
                COALESCE(e.nome, 'Máquina') as machine_name
            FROM deteccoes d
            LEFT JOIN sessoes s ON d.id_sessao = s.id_sessao
            LEFT JOIN equipes e ON s.id_equipe = e.id_equipe
            ORDER BY d.detectado_em DESC
            LIMIT ?
        """.trimIndent()

        val history = mutableListOf<ScanRecord>()

        try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, limit)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val resultValue = if (rs.getBoolean("result")) "SUCCESS" else "FAILED"
                history.add(
                    ScanRecord(
                        id = rs.getString("id"),
                        qrCode = rs.getString("qr_code"),
                        userId = null,
                        scannedAt = rs.getString("scanned_at"),
                        machineId = "mac_001",
                        machineName = rs.getString("machine_name"),
                        result = resultValue
                    )
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return history
    }


    fun getAllProductStock(): List<ProductStockDetail> {
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT 
                p.id_produto,
                p.nome,
                p.sku,
                p.quantidade,
                COALESCE(c.nome, 'OUTROS') as categoria
            FROM produtos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            ORDER BY p.nome
        """.trimIndent()

        val products = mutableListOf<ProductStockDetail>()

        try {
            val stmt = connection.prepareStatement(sql)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val quantity = rs.getInt("quantidade")
                val minStock = 10
                val status = when {
                    quantity == 0 -> "OUT_OF_STOCK"
                    quantity <= minStock -> "LOW_STOCK"
                    else -> "IN_STOCK"
                }

                products.add(
                    ProductStockDetail(
                        id = "prd_${String.format("%03d", rs.getInt("id_produto"))}",
                        name = rs.getString("nome"),
                        sku = rs.getString("sku"),
                        quantity = quantity,
                        minStock = minStock,
                        status = status
                    )
                )
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.close()
        }

        return products
    }
}