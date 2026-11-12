package com.apz.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val APZColors = darkColorScheme(
    primary = Color(0xFF9B72FF),
    secondary = Color(0xFF00D7F5),
    background = Color(0xFF0B0E1A),
    surface = Color(0xFF151A2B)
)

@Composable
fun APZTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = APZColors, content = content)
}
