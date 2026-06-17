package com.saidcharoun.tahaddisighar

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF6A1B9A)
private val PurpleDark = Color(0xFF4A148C)
private val Amber = Color(0xFFFFC107)
private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

val CorrectGreen = Green
val WrongRed = Red
val AccentAmber = Amber

private val LightColors = lightColorScheme(
    primary = Purple,
    secondary = Amber,
    background = Color(0xFFF3E5F5),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Amber,
    secondary = Purple,
    background = PurpleDark,
    surface = Color(0xFF311B92)
)

@Composable
fun TahaddiTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
