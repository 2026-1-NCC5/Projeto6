package com.AlimempatIA.stockai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.AlimempatIA.stockai.domain.auth.UserStore
import com.AlimempatIA.stockai.domain.model.UserRole
import com.AlimempatIA.stockai.navigation.Routes
import com.AlimempatIA.stockai.ui.theme.*

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Filled.Dashboard, Routes.DASHBOARD),
    BottomNavItem("Estoque", Icons.Filled.Inventory2, Routes.INVENTORY),
    BottomNavItem("Câmera", Icons.Filled.CameraAlt, Routes.CAMERA),
    BottomNavItem("Relatórios", Icons.Filled.BarChart, Routes.REPORTS),
    BottomNavItem("Config.", Icons.Filled.Settings, Routes.SETTINGS)
)

@Composable
fun AppBottomNavBar(navController: NavController) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route
    val role = UserStore.currentUser?.role

    // Relatórios visível para todos exceto OPERADOR
    val visibleItems = bottomNavItems.filter { item ->
        if (item.route == Routes.REPORTS) role != null && role != UserRole.OPERADOR
        else true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
    ) {
        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleItems.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            navController.navigate(item.route) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 40.dp else 36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PrimaryBlue.copy(alpha = 0.2f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) PrimaryBlue else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PrimaryBlue else TextSecondary
                    )
                }
            }
        }
    }
}
