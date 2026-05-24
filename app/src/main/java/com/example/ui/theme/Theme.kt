package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Pink,
    onSecondary = Color.White,
    tertiary = PurpleLight,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextMain,
    surface = CardColor,
    onSurface = TextMain,
    surfaceVariant = Panel,
    onSurfaceVariant = Muted
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
