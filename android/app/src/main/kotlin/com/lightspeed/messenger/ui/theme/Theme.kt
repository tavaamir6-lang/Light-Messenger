package com.lightspeed.messenger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFF2E8BFF),
    secondary = Color(0xFFFFC857),
    background = Color(0xFF0B0D10),
    surface = Color(0xFF11151B)
)

@Composable
fun LightMessengerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
