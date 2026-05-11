package com.AlimempatIA.stockai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AlimempatIA.stockai.domain.model.StockStatus
import com.AlimempatIA.stockai.ui.theme.*

@Composable
fun StockStatusBadge(status: StockStatus) {
    val (bgColor, textColor, label) = when (status) {
        StockStatus.IN_STOCK -> Triple(StatusGreenDim, StatusGreen, status.label)
        StockStatus.LOW_STOCK -> Triple(StatusYellowDim, StatusYellow, status.label)
        StockStatus.OUT_OF_STOCK -> Triple(StatusRedDim, StatusRed, status.label)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = textColor
        )
    }
}
