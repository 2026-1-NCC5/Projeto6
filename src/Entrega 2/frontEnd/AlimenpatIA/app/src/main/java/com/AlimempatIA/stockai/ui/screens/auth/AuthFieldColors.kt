package com.AlimempatIA.stockai.ui.screens.auth

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.AlimempatIA.stockai.ui.theme.*

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardDarkElevated,
    unfocusedContainerColor = CardDarkElevated,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = DividerColor,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = PrimaryBlue
)
