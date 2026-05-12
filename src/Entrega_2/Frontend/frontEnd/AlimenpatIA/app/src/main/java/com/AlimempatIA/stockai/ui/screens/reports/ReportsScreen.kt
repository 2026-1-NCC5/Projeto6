package com.AlimempatIA.stockai.ui.screens.reports

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.AlimempatIA.stockai.ui.components.StockStatusBadge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.AlimempatIA.stockai.domain.model.Trend
import com.AlimempatIA.stockai.ui.components.AppBottomNavBar
import com.AlimempatIA.stockai.ui.theme.*
import com.AlimempatIA.stockai.ui.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val attributeMap by viewModel.attributeMap.collectAsStateWithLifecycle()
    val trendCounts by viewModel.trendCounts.collectAsStateWithLifecycle()
    val roles by viewModel.roles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedRole by remember { mutableStateOf<String?>(null) }
    var selectedTrend by remember { mutableStateOf<Trend?>(null) }

    fun applyFilters() {
        val trendStr = when (selectedTrend) {
            Trend.UP -> "UP"
            Trend.STABLE -> "STABLE"
            Trend.DOWN -> "DOWN"
            null -> null
        }
        viewModel.filterMembers(selectedRole, trendStr)
    }

    LaunchedEffect(selectedRole, selectedTrend) {
        applyFilters()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = { ReportsTopBar() },
        bottomBar = { AppBottomNavBar(navController = navController) }
    ) { pv ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Erro: $error", color = StatusRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pv)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Summary
                    summary?.let { summaryData ->
                        item {
                            AttributeSummary(
                                members = members,
                                summary = summaryData
                            )
                        }
                    }

                    // Attribute Map
                    if (attributeMap.isNotEmpty()) {
                        item {
                            AttributeMapPanel(attributeMap = attributeMap)
                        }
                    }

                    // Insight Strip (trend counts)
                    trendCounts?.let { counts ->
                        item {
                            InsightStrip(trendCounts = counts)
                        }
                    }

                    // Filter Section
                    if (roles.isNotEmpty()) {
                        item {
                            FilterSection(
                                roles = roles,
                                selectedRole = selectedRole,
                                onRole = { selectedRole = it },
                                selectedTrend = selectedTrend,
                                onTrend = { selectedTrend = it }
                            )
                        }
                    }

                    // Members list
                    items(members, key = { it.id }) { member ->
                        MemberCard(
                            member = member,
                            onClick = {
                                navController.navigate("reports/member/${member.id}")
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}