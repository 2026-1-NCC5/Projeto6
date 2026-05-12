package com.alimenpatia.database

import com.alimenpatia.models.*
import java.time.Instant

object InventoryDatabaseMySQL {

    fun getAllProducts(
        search: String? = null,
        category: String? = null,
        status: String? = null
    ): List<Product> {
        println("=== InventoryDatabaseMySQL.getAllProducts() CHAMADA! ===")

        val products = mutableListOf<Product>()
        val connection = DatabaseFactory.getConnection()

        var sql = """
            SELECT p.id_produto, p.nome, p.sku, p.quantidade, p.atualizado_em, c.nome as categoria
            FROM produtos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            WHERE 1=1
        """.trimIndent()

        val params = mutableListOf<Any>()

        if (!search.isNullOrBlank()) {
            sql += " AND (p.nome LIKE ? OR p.sku LIKE ?)"
            params.add("%$search%")
            params.add("%$search%")
        }

        if (!category.isNullOrBlank()) {
            sql += " AND c.nome = ?"
            params.add(category)
        }

        sql += " ORDER BY p.nome ASC"

        return try {
            val stmt = connection.prepareStatement(sql)
            params.forEachIndexed { index, value ->
                stmt.setString(index + 1, value.toString())
            }
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val categoryName = rs.getString("categoria") ?: "OUTROS"
                val productCategory = when (categoryName.uppercase()) {
                    "CEREAIS" -> ProductCategory.CEREAIS
                    "LEGUMINOSAS" -> ProductCategory.LEGUMINOSAS
                    "MASSAS" -> ProductCategory.MASSAS
                    "OLEOS" -> ProductCategory.OLEOS
                    "LATICINIOS" -> ProductCategory.LATICINIOS
                    else -> ProductCategory.CEREAIS
                }

                val quantity = rs.getInt("quantidade")
                val minStock = when (productCategory) {
                    ProductCategory.CEREAIS -> 20
                    ProductCategory.LEGUMINOSAS -> 15
                    ProductCategory.MASSAS -> 10
                    ProductCategory.OLEOS -> 5
                    ProductCategory.LATICINIOS -> 10
                }

                products.add(
                    Product(
                        id = "prd_${String.format("%03d", rs.getInt("id_produto"))}",
                        name = rs.getString("nome"),
                        sku = rs.getString("sku"),
                        category = productCategory,
                        quantity = quantity,
                        minStock = minStock,
                        detectedAt = rs.getString("atualizado_em") ?: Instant.now().toString()
                    )
                )
            }
            connection.close()

            println("=== Produtos carregados do MySQL: ${products.size} ===")

            var filtered: List<Product> = products
            if (!status.isNullOrBlank()) {
                filtered = products.filter { product ->
                    when (status) {
                        "IN_STOCK" -> product.quantity > product.minStock
                        "LOW_STOCK" -> product.quantity in 1..product.minStock
                        "OUT_OF_STOCK" -> product.quantity == 0
                        else -> true
                    }
                }
                println("Filtro de status: $status -> ${filtered.size} produtos")
            } else {
                println("Sem filtro de status -> ${filtered.size} produtos")
            }

            filtered
        } catch (e: Exception) {
            println("Erro ao buscar produtos: ${e.message}")
            e.printStackTrace()
            connection.close()
            emptyList()
        }
    }

    fun getProductById(id: String): Product? {
        val productId = id.replace("prd_", "").toIntOrNull() ?: return null
        val connection = DatabaseFactory.getConnection()
        val sql = """
            SELECT p.id_produto, p.nome, p.sku, p.quantidade, p.atualizado_em, c.nome as categoria
            FROM produtos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            WHERE p.id_produto = ?
        """.trimIndent()

        return try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, productId)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val categoryName = rs.getString("categoria") ?: "OUTROS"
                val productCategory = when (categoryName.uppercase()) {
                    "CEREAIS" -> ProductCategory.CEREAIS
                    "LEGUMINOSAS" -> ProductCategory.LEGUMINOSAS
                    "MASSAS" -> ProductCategory.MASSAS
                    "OLEOS" -> ProductCategory.OLEOS
                    "LATICINIOS" -> ProductCategory.LATICINIOS
                    else -> ProductCategory.CEREAIS
                }

                val quantity = rs.getInt("quantidade")
                val minStock = when (productCategory) {
                    ProductCategory.CEREAIS -> 20
                    ProductCategory.LEGUMINOSAS -> 15
                    ProductCategory.MASSAS -> 10
                    ProductCategory.OLEOS -> 5
                    ProductCategory.LATICINIOS -> 10
                }

                val product = Product(
                    id = "prd_${String.format("%03d", rs.getInt("id_produto"))}",
                    name = rs.getString("nome"),
                    sku = rs.getString("sku"),
                    category = productCategory,
                    quantity = quantity,
                    minStock = minStock,
                    detectedAt = rs.getString("atualizado_em") ?: Instant.now().toString()
                )
                connection.close()
                product
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

    fun getCameraStatus(): CameraStatus {
        return CameraStatus(
            id = "cam_001",
            name = "Camera 01",
            location = "Esteira Principal",
            status = "DETECTING"
        )
    }

    fun getInventoryStats(products: List<Product>): InventoryStats {
        return InventoryStats(
            total = products.size,
            inStock = products.count { it.quantity > it.minStock },
            lowStock = products.count { it.quantity in 1..it.minStock },
            outOfStock = products.count { it.quantity == 0 }
        )
    }

    fun getAvailableCategories(): List<String> {
        val connection = DatabaseFactory.getConnection()
        val sql = "SELECT nome FROM categorias ORDER BY nome"

        return try {
            val stmt = connection.prepareStatement(sql)
            val rs = stmt.executeQuery()
            val categories = mutableListOf<String>()
            while (rs.next()) {
                categories.add(rs.getString("nome").uppercase())
            }
            connection.close()
            categories
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            listOf("CEREAIS", "LEGUMINOSAS", "MASSAS", "OLEOS", "LATICINIOS")
        }
    }

    fun getAvailableStatuses(): List<String> {
        return listOf("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK")
    }

    fun getStockLevel(product: Product): StockLevel {
        val progress = if (product.minStock > 0) {
            (product.quantity.toFloat() / (product.minStock * 3).toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        return StockLevel(
            current = product.quantity,
            minimum = product.minStock,
            progress = progress
        )
    }

    fun getAiDetection(product: Product): AiDetection {
        return AiDetection(
            cameraId = "cam_001",
            cameraName = "Camera 01",
            location = "Esteira Principal",
            confidence = 98.4,
            detectedAt = product.detectedAt
        )
    }

    fun updateProduct(id: String, request: UpdateProductRequest): Boolean {
        val productId = id.replace("prd_", "").toIntOrNull() ?: return false
        val connection = DatabaseFactory.getConnection()

        val categoryId = when (request.category.uppercase()) {
            "CEREAIS" -> 1
            "LEGUMINOSAS" -> 2
            "MASSAS" -> 3
            "OLEOS" -> 4
            "LATICINIOS" -> 5
            else -> 1
        }

        val sql = "UPDATE produtos SET nome = ?, sku = ?, id_categoria = ? WHERE id_produto = ?"

        return try {
            val stmt = connection.prepareStatement(sql)
            stmt.setString(1, request.name)
            stmt.setString(2, request.sku)
            stmt.setInt(3, categoryId)
            stmt.setInt(4, productId)
            val rows = stmt.executeUpdate()
            connection.close()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            false
        }
    }

    fun adjustStock(id: String, quantity: Int): Boolean {
        val productId = id.replace("prd_", "").toIntOrNull() ?: return false
        val connection = DatabaseFactory.getConnection()

        val getSql = "SELECT quantidade FROM produtos WHERE id_produto = ?"
        var currentQuantity = 0

        try {
            val getStmt = connection.prepareStatement(getSql)
            getStmt.setInt(1, productId)
            val rs = getStmt.executeQuery()
            if (rs.next()) {
                currentQuantity = rs.getInt("quantidade")
            }
            rs.close()
            getStmt.close()
        } catch (e: Exception) {
            connection.close()
            return false
        }

        val newQuantity = (currentQuantity + quantity).coerceAtLeast(0)
        val sql = "UPDATE produtos SET quantidade = ?, atualizado_em = NOW() WHERE id_produto = ?"

        return try {
            val stmt = connection.prepareStatement(sql)
            stmt.setInt(1, newQuantity)
            stmt.setInt(2, productId)
            val rows = stmt.executeUpdate()
            connection.close()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            connection.close()
            false
        }
    }
}