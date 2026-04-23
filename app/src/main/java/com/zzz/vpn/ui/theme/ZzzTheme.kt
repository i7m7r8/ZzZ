package com.zzz.vpn.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF003543),
    secondary = Color(0xFF82B1FF),
    background = Color(0xFF0A0E1A),
    surface = Color(0xFF111827),
    onBackground = Color(0xFFE0E6F0),
    onSurface = Color(0xFFE0E6F0),
    error = Color(0xFFFF5252)
)

@Composable
fun ZzzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
