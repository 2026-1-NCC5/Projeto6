package com.AlimempatIA.stockai.ui.screens.reports

import androidx.compose.ui.graphics.Color
import com.AlimempatIA.stockai.data.api.dto.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AlimempatIA.stockai.domain.model.*
import com.AlimempatIA.stockai.ui.theme.*

// ==================== TOP BAR ====================
@Composable
fun ReportsTopBar() {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentCyan.copy(alpha = 0.16f))
                    .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.BarChart, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Relatorios", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Evolucao de atributos observados pelo lider", fontSize = 12.sp, color = TextSecondary)
            }
        }
        HorizontalDivider(color = DividerColor)
    }
}

// ==================== ATTRIBUTE SUMMARY (com ReportsSummary) ====================
@Composable
fun AttributeSummary(members: List<MemberRecord>, summary: ReportsSummary) {
    if (members.isEmpty()) return

    ReportSurface(accentColor = AccentCyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(AccentCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Leitura do lider", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Media dos atributos da equipe", color = TextSecondary, fontSize = 12.sp)
            }
            Text("${summary.teamAttributeAverage}", color = StatusGreen, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.TrendingUp,
                label = "Evolucao media",
                value = formatDelta(summary.averageDelta),
                color = if (summary.averageDelta >= 0) StatusGreen else StatusRed
            )
            SummaryMetric(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.EmojiEvents,
                label = "Maior avanco",
                value = summary.bestEvolutionMemberId,
                color = StatusYellow
            )
        }

        Spacer(Modifier.height(10.dp))

        SummaryMetric(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Speed,
            label = "Pontos de atencao",
            value = "${summary.attentionCount} membro(s) pedem acompanhamento",
            color = if (summary.attentionCount > 0) StatusRed else StatusGreen
        )
    }
}

@Composable
private fun SummaryMetric(modifier: Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(CardDark).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ==================== ATTRIBUTE MAP PANEL (com AttributeMapDto) ====================
@Composable
fun AttributeMapPanel(attributeMap: List<AttributeMapDto>) {
    if (attributeMap.isEmpty()) return

    ReportSurface(accentColor = PrimaryBlue) {
        SectionHeader(title = "Mapa de atributos", subtitle = "Media observada pelo lider")
        Spacer(Modifier.height(14.dp))
        attributeMap.forEachIndexed { index, attribute ->
            AttributeProgressRow(
                name = attribute.name,
                score = attribute.score,
                previousScore = attribute.previousScore,
                color = try {
                    Color(android.graphics.Color.parseColor(attribute.color))
                } catch (e: Exception) {
                    PrimaryBlue
                }
            )
            if (index != attributeMap.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

// ==================== ATTRIBUTE PROGRESS ROW ====================
@Composable
fun AttributeProgressRow(name: String, score: Int, previousScore: Int, color: Color) {
    val progress = (score / 100f).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(score.toString(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDelta(score - previousScore), color = deltaColor(score - previousScore), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = color,
                trackColor = CardDarkElevated
            )
        }
    }
}

// ==================== INSIGHT STRIP (com TrendCountsDto) ====================
@Composable
fun InsightStrip(trendCounts: TrendCountsDto) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InsightPill(Modifier.weight(1f), "${trendCounts.evolving}", "evoluindo", StatusGreen, Icons.Filled.TrendingUp)
        InsightPill(Modifier.weight(1f), "${trendCounts.maintaining}", "mantendo", StatusYellow, Icons.Filled.TrendingFlat)
        InsightPill(Modifier.weight(1f), "${trendCounts.attention}", "atenção", StatusRed, Icons.Filled.TrendingDown)
    }
}

@Composable
private fun InsightPill(modifier: Modifier, value: String, label: String, color: Color, icon: ImageVector) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        Column {
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

// ==================== FILTER SECTION ====================
@Composable
fun FilterSection(
    roles: List<String>,
    selectedRole: String?,
    onRole: (String?) -> Unit,
    selectedTrend: Trend?,
    onTrend: (Trend?) -> Unit
) {
    ReportSurface(accentColor = DividerColor) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.FilterAlt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Text("Filtros", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip("Todos", selectedRole == null, AccentCyan) { onRole(null) } }
            items(roles) { role ->
                FilterChip(role, selectedRole == role, AccentCyan) { onRole(role) }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip("Toda evolucao", selectedTrend == null, AccentCyan) { onTrend(null) } }
            item { FilterChip("Evoluindo", selectedTrend == Trend.UP, StatusGreen) { onTrend(Trend.UP) } }
            item { FilterChip("Mantendo", selectedTrend == Trend.STABLE, StatusYellow) { onTrend(Trend.STABLE) } }
            item { FilterChip("Atencao", selectedTrend == Trend.DOWN, StatusRed) { onTrend(Trend.DOWN) } }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.18f) else CardDark)
            .border(1.dp, if (selected) color.copy(alpha = 0.55f) else DividerColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) color else TextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

// ==================== MEMBER CARD ====================
@Composable
fun MemberCard(member: MemberRecord, onClick: () -> Unit) {
    ReportSurface(
        accentColor = member.roleColor,
        modifier = Modifier.clickable { onClick() },
        innerPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InitialsAvatar(member)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(member.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoleBadge(member.role, member.roleColor)
                    Text(member.joinDate, fontSize = 10.sp, color = TextSecondary)
                }
            }
            TrendBadge(member.trend)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardDark).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KpiBlock(member.currentScore.toString(), "Media", PrimaryBlue)
            KpiBlock(formatDelta(member.scoreDelta), "Evolucao", deltaColor(member.scoreDelta))
            KpiBlock(member.developmentAttribute.name, "Foco", StatusYellow)
        }

        Spacer(Modifier.height(14.dp))

        member.attributes.take(3).forEachIndexed { index, attribute ->
            AttributeProgressRow(
                name = attribute.name,
                score = attribute.score,
                previousScore = attribute.previousScore,
                color = attribute.color
            )
            if (index != 2) Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Historico da media", fontSize = 11.sp, color = TextSecondary)
            Text("Destaque: ${member.strongestAttribute.name}", fontSize = 11.sp, color = member.strongestAttribute.color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Sparkline(data = member.weeklyEvolution, color = member.roleColor)
    }
}

@Composable
private fun InitialsAvatar(member: MemberRecord) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(member.roleColor.copy(alpha = 0.13f))
            .border(1.5.dp, member.roleColor.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            member.name.split(" ").take(2).joinToString("") { it.first().uppercase() },
            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = member.roleColor
        )
    }
}

@Composable
private fun RoleBadge(role: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(role, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TrendBadge(trend: Trend) {
    val color = trendColor(trend)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(trendIcon(trend), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(trendLabel(trend), fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KpiBlock(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Sparkline(data: List<Int>, color: Color, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val maxValue = (data.maxOrNull() ?: 0).toFloat()
    val minValue = (data.minOrNull() ?: 0).toFloat()

    Canvas(modifier = modifier.fillMaxWidth().height(50.dp)) {
        val range = (maxValue - minValue).coerceAtLeast(1f)
        val step = size.width / (data.size - 1).coerceAtLeast(1).toFloat()
        val line = Path()

        data.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - ((value - minValue) / range) * size.height * 0.78f - size.height * 0.08f
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }

        val fill = Path().apply {
            addPath(line)
            lineTo((data.size - 1) * step, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)))
        drawPath(line, color = color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        data.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - ((value - minValue) / range) * size.height * 0.78f - size.height * 0.08f
            drawCircle(color, 3.dp.toPx(), Offset(x, y))
            drawCircle(SurfaceDark, 1.4.dp.toPx(), Offset(x, y))
        }
    }
}

// ==================== HELPERS ====================
fun trendColor(trend: Trend): Color = when (trend) {
    Trend.UP -> StatusGreen
    Trend.STABLE -> StatusYellow
    Trend.DOWN -> StatusRed
}

fun trendLabel(trend: Trend): String = when (trend) {
    Trend.UP -> "Evoluindo"
    Trend.STABLE -> "Mantendo"
    Trend.DOWN -> "Atencao"
}

fun trendIcon(trend: Trend): ImageVector = when (trend) {
    Trend.UP -> Icons.Filled.TrendingUp
    Trend.STABLE -> Icons.Filled.TrendingFlat
    Trend.DOWN -> Icons.Filled.TrendingDown
}

fun deltaColor(delta: Int): Color = when {
    delta > 0 -> StatusGreen
    delta < 0 -> StatusRed
    else -> StatusYellow
}

fun formatDelta(delta: Int): String = if (delta > 0) "+$delta" else delta.toString()
fun formatDelta(delta: Double): String = formatDelta(kotlin.math.round(delta).toInt())

val weeklyLabels = listOf("S-6", "S-5", "S-4", "S-3", "S-2", "S-1")
val monthlyLabels = listOf("Nov", "Dez", "Jan", "Fev", "Mar", "Abr")

// ==================== COMMON COMPONENTS ====================
@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun ReportSurface(
    accentColor: Color,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(accentColor))
        Column(modifier = Modifier.padding(innerPadding), content = content)
    }
}