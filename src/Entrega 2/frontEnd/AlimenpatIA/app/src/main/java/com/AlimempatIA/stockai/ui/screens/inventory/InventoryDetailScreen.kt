package com.AlimempatIA.stockai.ui.screens.inventory

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.AlimempatIA.stockai.domain.model.StockStatus
import com.AlimempatIA.stockai.ui.components.StockStatusBadge
import com.AlimempatIA.stockai.ui.theme.*
import com.AlimempatIA.stockai.ui.viewmodel.InventoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    navController: NavController,
    productId: String,
    viewModel: InventoryViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


    LaunchedEffect(productId) {
        if (products.isEmpty()) {
            viewModel.loadInventory()
        }
    }

    val product = products.find { it.id == productId }


    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editMinStock by remember { mutableStateOf("") }


    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustQuantity by remember { mutableStateOf("") }

    LaunchedEffect(product) {
        if (product != null) {
            editName = product.name
            editMinStock = product.minStock.toString()
        }
    }

    if (isLoading && product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Produto não encontrado", color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Voltar")
                }
            }
        }
        return
    }

    val accentColor = when (product.status) {
        StockStatus.IN_STOCK -> StatusGreen
        StockStatus.LOW_STOCK -> StatusYellow
        StockStatus.OUT_OF_STOCK -> StatusRed
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text(product.name, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            if (isEditing) Icons.Filled.Close else Icons.Filled.Edit,
                            contentDescription = if (isEditing) "Cancelar" else "Editar",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { pv ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Produto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            StockStatusBadge(status = product.status)
                        }

                        if (isEditing) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nome") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = DividerColor
                                )
                            )
                            OutlinedTextField(
                                value = editMinStock,
                                onValueChange = { editMinStock = it },
                                label = { Text("Estoque Mínimo") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = DividerColor
                                )
                            )
                            Button(
                                onClick = {
                                    isEditing = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Salvar Alterações")
                            }
                        } else {
                            Text("SKU: ${product.sku}", color = TextSecondary)
                            Text("Categoria: ${product.category.label}", color = TextSecondary)
                            Text("Estoque Mínimo: ${product.minStock} unid.", color = TextSecondary)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Nível de Estoque", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quantidade atual:", color = TextSecondary)
                            Text(
                                "${product.quantity} unid.",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (product.quantity.toFloat() / (product.minStock * 3).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                            color = accentColor,
                            trackColor = DividerColor
                        )

                        Button(
                            onClick = { showAdjustDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AIGlow)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajustar Estoque")
                        }
                    }
                }
            }


            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Detecção por IA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Câmera: Camera 01 - Esteira Principal", color = TextSecondary)
                        Text("Confiança: 98.4%", color = StatusGreen)
                        Text("Detectado em: ${product.formattedDetectedDate}", color = TextSecondary)
                    }
                }
            }
        }
    }


    if (showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Ajustar Estoque", color = TextPrimary) },
            text = {
                Column {
                    Text("Produto: ${product.name}", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustQuantity,
                        onValueChange = { adjustQuantity = it },
                        label = { Text("Quantidade") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DividerColor
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quantity = adjustQuantity.toIntOrNull() ?: 0
                        showAdjustDialog = false
                        adjustQuantity = ""
                    }
                ) {
                    Text("Confirmar", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}