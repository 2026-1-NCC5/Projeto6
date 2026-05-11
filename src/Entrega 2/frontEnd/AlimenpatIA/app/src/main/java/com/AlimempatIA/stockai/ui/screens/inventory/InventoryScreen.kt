package com.AlimempatIA.stockai.ui.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.AlimempatIA.stockai.data.api.dto.CameraStatusDto
import com.AlimempatIA.stockai.domain.model.Product
import com.AlimempatIA.stockai.domain.model.ProductCategory
import com.AlimempatIA.stockai.domain.model.StockStatus
import com.AlimempatIA.stockai.ui.components.AppBottomNavBar
import com.AlimempatIA.stockai.ui.components.AIStatusIndicator
import com.AlimempatIA.stockai.ui.components.StockStatusBadge
import com.AlimempatIA.stockai.ui.theme.*
import com.AlimempatIA.stockai.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(navController: NavController) {
    val viewModel: InventoryViewModel = viewModel()

    val products by viewModel.products.collectAsState()
    val cameraStatus by viewModel.cameraStatus.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(searchQuery, selectedCategory, selectedStatus) {
        viewModel.loadInventory(
            search = searchQuery.takeIf { it.isNotBlank() },
            category = selectedCategory,
            status = selectedStatus
        )
    }

    val filteredProducts = products

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = { AppBottomNavBar(navController = navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                InventoryHeader(cameraStatus = cameraStatus)
            }

            item {
                if (stats != null) {
                    InventoryStatsRow(
                        total = stats!!.total,
                        inStock = stats!!.inStock,
                        lowStock = stats!!.lowStock,
                        outOfStock = stats!!.outOfStock
                    )
                } else {
                    Box(modifier = Modifier.height(100.dp))
                }
            }

            item {
                InventorySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            item {
                if (availableCategories.isNotEmpty()) {
                    CategoryFilterRow(
                        categories = availableCategories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }
            }

            item {
                if (availableCategories.isNotEmpty()) {
                    StatusFilterRow(
                        statuses = listOf("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"),
                        selectedStatus = selectedStatus,
                        onStatusSelected = { selectedStatus = it }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredProducts.size} produto(s) encontrado(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            items(filteredProducts, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = {
                        navController.navigate("inventory_detail/${product.id}")
                    }
                )
            }

            if (isLoading && filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun InventoryHeader(cameraStatus: CameraStatusDto?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, BackgroundDark)
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Controle de",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Estoque",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AIStatusIndicator(isActive = true)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .clickable {},
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificações",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AIGlow.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = AIGlowLight,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (cameraStatus != null) {
                        "${cameraStatus.name} — ${cameraStatus.location} • ${cameraStatus.status}"
                    } else {
                        "Câmera 01 — Esteira Principal • Detectando"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AIGlowLight
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = Color.White,
                    containerColor = CardDark,
                    labelColor = TextSecondary
                )
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                label = {
                    Text(
                        when (category) {
                            "CEREAIS" -> "Cereais"
                            "LEGUMINOSAS" -> "Leguminosas"
                            "MASSAS" -> "Massas"
                            "OLEOS" -> "Óleos"
                            "LATICINIOS" -> "Laticínios"
                            else -> category
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = Color.White,
                    containerColor = CardDark,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun StatusFilterRow(
    statuses: List<String>,
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(statuses) { status ->
            val color = when (status) {
                "IN_STOCK" -> StatusGreen
                "LOW_STOCK" -> StatusYellow
                "OUT_OF_STOCK" -> StatusRed
                else -> StatusGreen
            }
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(if (selectedStatus == status) null else status) },
                label = {
                    Text(
                        when (status) {
                            "IN_STOCK" -> "Em Estoque"
                            "LOW_STOCK" -> "Estoque Baixo"
                            "OUT_OF_STOCK" -> "Sem Estoque"
                            else -> status
                        }
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.25f),
                    selectedLabelColor = color,
                    containerColor = CardDark,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun InventoryStatsRow(
    total: Int,
    inStock: Int,
    lowStock: Int,
    outOfStock: Int
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCardSmall(
                title = "Total",
                value = total.toString(),
                icon = Icons.Filled.Inventory2,
                accentColor = PrimaryBlue
            )
        }
        item {
            StatCardSmall(
                title = "Em Estoque",
                value = inStock.toString(),
                icon = Icons.Filled.CheckCircle,
                accentColor = StatusGreen
            )
        }
        item {
            StatCardSmall(
                title = "Estoque Baixo",
                value = lowStock.toString(),
                icon = Icons.Filled.Warning,
                accentColor = StatusYellow
            )
        }
        item {
            StatCardSmall(
                title = "Sem Estoque",
                value = outOfStock.toString(),
                icon = Icons.Filled.Cancel,
                accentColor = StatusRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = {
            Text(
                text = "Buscar por nome ou SKU...",
                color = TextDisabled
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Limpar", tint = TextSecondary)
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardDark,
            unfocusedContainerColor = CardDark,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = DividerColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = PrimaryBlue
        ),
        singleLine = true
    )
}

@Composable
private fun StatCardSmall(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                title,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val accentColor = when (product.status) {
        StockStatus.IN_STOCK -> StatusGreen
        StockStatus.LOW_STOCK -> StatusYellow
        StockStatus.OUT_OF_STOCK -> StatusRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(accentColor)
                .align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (product.category) {
                        ProductCategory.CEREAIS -> Icons.Filled.Grain
                        ProductCategory.LEGUMINOSAS -> Icons.Filled.Spa
                        ProductCategory.MASSAS -> Icons.Filled.RamenDining
                        ProductCategory.OLEOS -> Icons.Filled.WaterDrop
                        ProductCategory.LATICINIOS -> Icons.Filled.LocalDrink
                    },
                    contentDescription = null,
                    tint = PrimaryBlueLight,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.sku,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StockStatusBadge(status = product.status)
                    Text(
                        text = "• ${product.category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = product.quantity.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "unid.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = product.formattedDetectedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabled
                    )
                }
            }
        }
    }
}