package com.AlimempatIA.stockai.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.AlimempatIA.stockai.ui.components.AppBottomNavBar
import com.AlimempatIA.stockai.ui.theme.*
import com.AlimempatIA.stockai.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val dashboardData = viewModel.dashboardData
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = { AppBottomNavBar(navController = navController) }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            error != null && dashboardData == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Erro: $error", color = StatusRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDashboardData() }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }

            dashboardData != null -> {
                val data = dashboardData!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    item {
                        Column {
                            Text("Dashboard", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Visão geral da equipe", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                    item {
                        // Stat Cards
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DashStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Reconhecidos",
                                    value = data.recognized.toString(),
                                    icon = Icons.Filled.CheckCircle,
                                    accentColor = StatusGreen
                                )
                                DashStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Não detectados",
                                    value = data.notRecognized.toString(),
                                    icon = Icons.Filled.Cancel,
                                    accentColor = StatusRed
                                )
                            }
                            DashStatCard(
                                modifier = Modifier.fillMaxWidth(),
                                title = "Total de produtos — Equipe",
                                value = data.totalTeam.toString(),
                                icon = Icons.Filled.Inventory2,
                                accentColor = PrimaryBlue,
                                isWide = true
                            )
                        }
                    }
                    item {
                        // Pie Chart
                        PieChartCard(recognized = data.recognized, notRecognized = data.notRecognized)
                    }
                    item {
                        // Bar Chart
                        BarChartCard(weeklyData = data.weeklyData)
                    }
                    item {
                        // Line Chart
                        LineChartCard(weeklyData = data.weeklyData)
                    }
                    item {
                        // Meta Card
                        MetaCard(goal = data.goal)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    isWide: Boolean = false
) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(CardDark)) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(accentColor))
        Row(
            modifier = Modifier
                .padding(16.dp)
                .let { if (isWide) it.fillMaxWidth() else it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    text = value,
                    fontSize = if (isWide) 28.sp else 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(title, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PieChartCard(recognized: Int, notRecognized: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Distribuição de Leituras", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Text("Reconhecidos vs Não reconhecidos", fontSize = 12.sp, color = TextSecondary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val total = (recognized + notRecognized).toFloat()
                val angleRec = if (total > 0) 360f * recognized / total else 0f

                androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                    val stroke = 30.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)

                    drawArc(
                        color = StatusGreen,
                        startAngle = -90f,
                        sweepAngle = angleRec,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = StatusRed.copy(alpha = 0.75f),
                        startAngle = -90f + angleRec,
                        sweepAngle = 360f - angleRec,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(StatusGreen, "Reconhecidos", "$recognized")
                    LegendItem(StatusRed, "Não reconhecidos", "$notRecognized")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
                    LegendItem(PrimaryBlue, "Total lido", "${recognized + notRecognized}")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun BarChartCard(weeklyData: com.AlimempatIA.stockai.data.api.dto.WeeklyDataDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Produtos por Dia", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Text("Últimos 7 dias", fontSize = 12.sp, color = TextSecondary)
            }

            val dadosBarra = weeklyData.dailyCounts.map { it.count }
            val maxVal = if (dadosBarra.isNotEmpty()) dadosBarra.maxOrNull()?.toFloat() ?: 1f else 1f
            val textMeasurer = rememberTextMeasurer()
            val labelStyle = TextStyle(color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                if (dadosBarra.isNotEmpty()) {
                    val count = dadosBarra.size
                    val spacing = size.width / (count * 2 + 1)
                    val barW = spacing

                    dadosBarra.forEachIndexed { i, v ->
                        val barH = (v / maxVal) * size.height * 0.75f
                        val x = spacing + i * (barW + spacing)
                        val barTop = size.height - barH

                        drawRoundRect(
                            color = PrimaryBlue,
                            topLeft = Offset(x, barTop),
                            size = androidx.compose.ui.geometry.Size(barW, barH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        val measured = textMeasurer.measure(v.toString(), labelStyle)
                        val labelX = x + (barW - measured.size.width) / 2f
                        val labelY = barTop - measured.size.height - 4.dp.toPx()
                        if (labelY > 0) {
                            drawText(measured, topLeft = Offset(labelX, labelY))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weeklyData.weekDays.forEach { Text(it, fontSize = 11.sp, color = TextSecondary) }
            }
        }
    }
}

@Composable
private fun LineChartCard(weeklyData: com.AlimempatIA.stockai.data.api.dto.WeeklyDataDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Evolução Acumulada", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Text("Total de produtos ao longo da semana", fontSize = 12.sp, color = TextSecondary)
            }

            val dadosLinha = weeklyData.cumulativeCounts.map { it.count }
            val maxVal = if (dadosLinha.isNotEmpty()) dadosLinha.maxOrNull()?.toFloat() ?: 1f else 1f

            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                if (dadosLinha.isNotEmpty() && dadosLinha.size > 1) {
                    val stepX = size.width / (dadosLinha.size - 1).toFloat()

                    // Filled area
                    val area = Path()
                    dadosLinha.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v / maxVal) * size.height * 0.85f
                        if (i == 0) area.moveTo(x, y) else area.lineTo(x, y)
                    }
                    area.lineTo((dadosLinha.size - 1) * stepX, size.height)
                    area.lineTo(0f, size.height)
                    area.close()
                    drawPath(area, color = AIGlow.copy(alpha = 0.15f))

                    // Line
                    val line = Path()
                    dadosLinha.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v / maxVal) * size.height * 0.85f
                        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                    }
                    drawPath(line, color = AIGlow, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Dots
                    dadosLinha.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v / maxVal) * size.height * 0.85f
                        drawCircle(AIGlow, 4.dp.toPx(), Offset(x, y))
                        drawCircle(CardDark, 2.dp.toPx(), Offset(x, y))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weeklyData.weekDays.forEach { Text(it, fontSize = 11.sp, color = TextSecondary) }
            }
        }
    }
}

@Composable
private fun MetaCard(goal: com.AlimempatIA.stockai.data.api.dto.GoalInfoDto) {
    val progress = goal.current.toFloat() / goal.target

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark)) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(StatusYellow))
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusYellow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = StatusYellow, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Meta da Equipe", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Text("${goal.current} / ${goal.target} produtos lidos", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Text("${goal.percentage}%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = StatusYellow)
            }

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                color = StatusYellow,
                trackColor = CardDarkElevated
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", fontSize = 11.sp, color = TextSecondary)
                Text("${goal.target} produtos", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}