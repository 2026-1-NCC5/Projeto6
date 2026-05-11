package com.AlimempatIA.stockai.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.AlimempatIA.stockai.ui.screens.admin.AdminScreen
import com.AlimempatIA.stockai.ui.screens.auth.LoginScreen
import com.AlimempatIA.stockai.ui.screens.auth.RegisterScreen
import com.AlimempatIA.stockai.ui.screens.camera.CameraScreen
import com.AlimempatIA.stockai.ui.screens.dashboard.DashboardScreen
import com.AlimempatIA.stockai.ui.screens.inventory.InventoryDetailScreen
import com.AlimempatIA.stockai.ui.screens.inventory.InventoryScreen
import com.AlimempatIA.stockai.ui.screens.reports.ReportsScreen
import com.AlimempatIA.stockai.ui.screens.settings.SettingsScreen
import com.AlimempatIA.stockai.ui.viewmodel.ReportsViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }

        composable(Routes.REGISTER) {
            RegisterScreen(navController)
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(navController)
        }

        composable(Routes.INVENTORY) {
            InventoryScreen(navController)
        }

        // CORREÇÃO: Adicionar o argumento productId
        composable(
            route = Routes.INVENTORY_DETAIL,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            InventoryDetailScreen(
                navController = navController,
                productId = productId
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(navController)
        }

        composable(Routes.REPORTS) {
            val reportsViewModel: ReportsViewModel = viewModel()
            ReportsScreen(
                navController = navController,
                viewModel = reportsViewModel
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController)
        }

        composable(Routes.ADMIN) {
            AdminScreen(navController)
        }
    }
}