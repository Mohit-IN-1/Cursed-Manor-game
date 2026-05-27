package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolishedColorScheme = lightColorScheme(
    primary = PolishedPurple,
    onPrimary = Color.White,
    primaryContainer = LightVioletCard,
    onPrimaryContainer = DeepVioletText,
    secondary = PolishedPurple,
    onSecondary = Color.White,
    tertiary = NeonGold,
    background = PolishedBackground,
    onBackground = DeepCharcoal,
    surface = PolishedNeutralCard,
    onSurface = DeepCharcoal,
    surfaceVariant = LightVioletCard,
    onSurfaceVariant = DeepVioletText,
    error = BloodyRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PolishedColorScheme,
        typography = Typography,
        content = content
    )
}

