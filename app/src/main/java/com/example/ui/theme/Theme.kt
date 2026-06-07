package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = AccentGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color(0xFFE9EDF0),
    onSurface = Color(0xFFE9EDF0),
    surfaceVariant = DarkSecondaryContainer,
    onSurfaceVariant = Color(0xFF8696A0)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = LightGreenBubble,
    onPrimaryContainer = Color(0xFF111B21),
    secondary = AccentGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = Color(0xFF111B21),
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF667781)
)

@Composable
fun GolpoKoriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
