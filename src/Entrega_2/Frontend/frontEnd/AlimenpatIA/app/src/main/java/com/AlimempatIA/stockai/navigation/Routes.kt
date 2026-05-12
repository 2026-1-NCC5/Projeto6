package com.AlimempatIA.stockai.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val INVENTORY = "inventory"
    const val INVENTORY_DETAIL = "inventory_detail/{productId}"  // ← COM O PARÂMETRO
    const val CAMERA = "camera"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"

    // Função auxiliar para navegar com parâmetro
    fun inventoryDetail(productId: String) = "inventory_detail/$productId"
}