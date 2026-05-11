package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.Instant
import java.util.UUID

object CameraDatabase {

    private val machines = mutableListOf<MachineInfo>()
    private val scanHistory = mutableListOf<ScanRecord>()
    private val products = mutableListOf<ProductStockDetail>()

    init {
        generateMockData()
    }

    private fun generateMockData() {
        // 生成产品库存数据
        products.addAll(
            listOf(
                ProductStockDetail("prd_001", "Arroz Branco 5kg", "ARR-5KG-001", 42, 20, "IN_STOCK"),
                ProductStockDetail("prd_002", "Feijão Carioca 1kg", "FEI-1KG-002", 8, 15, "LOW_STOCK"),
                ProductStockDetail("prd_003", "Macarrão Espaguete 500g", "MAC-500G-003", 0, 10, "OUT_OF_STOCK"),
                ProductStockDetail("prd_004", "Azeite de Oliva 500ml", "AZE-500ML-004", 15, 5, "IN_STOCK"),
                ProductStockDetail("prd_005", "Leite UHT 1L", "LEI-1L-005", 3, 10, "LOW_STOCK")
            )
        )

        // 生成机器数据
        machines.addAll(
            listOf(
                MachineInfo(
                    id = "mac_001",
                    name = "Esteira Principal",
                    code = "ESTEIRA-PRINCIPAL-01",
                    location = "Área de Produção",
                    status = MachineStatus.DETECTING.label,
                    lastScan = null,
                    productCount = 5,
                    products = products.map { p ->
                        ProductSummary(p.id, p.name, p.sku, p.quantity, p.status)
                    }
                ),
                MachineInfo(
                    id = "mac_002",
                    name = "Esteira Sul",
                    code = "ESTEIRA-SUL-02",
                    location = "Área de Expedição",
                    status = MachineStatus.ONLINE.label,
                    lastScan = null,
                    productCount = 0,
                    products = emptyList()
                ),
                MachineInfo(
                    id = "mac_003",
                    name = "Empilhadeira",
                    code = "EMPILHADEIRA-01",
                    location = "Armazém",
                    status = MachineStatus.OFFLINE.label,
                    lastScan = null,
                    productCount = 0,
                    products = emptyList()
                )
            )
        )
    }

    fun getAllMachines(): List<MachineInfo> {
        return machines.toList()
    }

    fun getMachineByQRCode(qrCode: String): MachineInfo? {
        return machines.find { it.code == qrCode }
    }

    fun getMachineById(id: String): MachineInfo? {
        return machines.find { it.id == id }
    }

    // 获取机器库存 - 用于 /camera/machines/{id}/stock 端点
    fun getMachineStock(machineId: String): MachineStock? {
        val machine = getMachineById(machineId) ?: return null

        val machineProducts = when (machineId) {
            "mac_001" -> products
            else -> emptyList()
        }

        val inStockCount = machineProducts.count { it.status == "IN_STOCK" }
        val lowStockCount = machineProducts.count { it.status == "LOW_STOCK" }
        val outOfStockCount = machineProducts.count { it.status == "OUT_OF_STOCK" }

        return MachineStock(
            machineId = machine.id,
            machineName = machine.name,
            machineLocation = machine.location,
            totalProducts = machineProducts.size,
            inStockCount = inStockCount,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            products = machineProducts
        )
    }

    // 获取扫描统计 - 用于 /camera/stats 端点
    fun getScanStats(): Map<String, Any> {
        val totalScans = scanHistory.size
        val successScans = scanHistory.count { it.result == ScanResult.SUCCESS.name }
        val uniqueMachines = scanHistory.map { it.machineId }.distinct().size
        val successRate = if (totalScans > 0) {
            successScans.toFloat() / totalScans * 100
        } else {
            0f
        }
        val lastScan = scanHistory.firstOrNull()?.scannedAt ?: ""

        return mapOf(
            "totalScans" to totalScans,
            "successScans" to successScans,
            "successRate" to successRate,
            "uniqueMachines" to uniqueMachines,
            "lastScan" to lastScan
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

        if (machine.status == MachineStatus.OFFLINE.label) {
            return QRScanResponse(
                success = false,
                message = "Máquina está offline. Não é possível escanear."
            )
        }

        if (machine.status == MachineStatus.MAINTENANCE.label) {
            return QRScanResponse(
                success = false,
                message = "Máquina em manutenção. Tente novamente mais tarde."
            )
        }

        // 注册扫描记录
        val scanRecord = ScanRecord(
            id = UUID.randomUUID().toString(),
            qrCode = qrCode,
            userId = userId,
            scannedAt = Instant.now().toString(),
            machineId = machine.id,
            machineName = machine.name,
            result = ScanResult.SUCCESS.name
        )
        scanHistory.add(0, scanRecord)

        // 更新机器的最后扫描时间
        val index = machines.indexOfFirst { it.id == machine.id }
        if (index != -1) {
            val updatedMachine = machine.copy(lastScan = Instant.now().toString())
            machines[index] = updatedMachine
        }

        return QRScanResponse(
            success = true,
            data = machine,
            message = "QR Code válido! Máquina: ${machine.name}"
        )
    }

    fun getScanHistory(limit: Int = 20): List<ScanRecord> {
        return scanHistory.take(limit)
    }

    fun getAllProductStock(): List<ProductStockDetail> {
        return products.toList()
    }
}