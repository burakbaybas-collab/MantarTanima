package com.burakbaybas.mantartanima.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForestGreen = Color(0xFF2E5E3A)
private val Moss = Color(0xFF7FA36B)
private val Cap = Color(0xFF8B5E3C)

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    secondary = Cap,
    tertiary = Moss,
)

private val DarkColors = darkColorScheme(
    primary = Moss,
    secondary = Color(0xFFD9B38C),
    tertiary = Color(0xFFA8C99A),
)

@Composable
fun MantarTanimaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
