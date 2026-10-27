package com.satguess.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF0B1A2A)
val GuessRed = Color(0xFFE53935)
val AnswerGreen = Color(0xFF2E7D32)

private val colors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00283A),
    secondary = Color(0xFF81C784),
    background = Night,
    surface = Color(0xFF132638),
    surfaceVariant = Color(0xFF1C3349),
    surfaceContainer = Color(0xFF132638),
    surfaceContainerHigh = Color(0xFF1C3349),
)

@Composable
fun SatGuessTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
