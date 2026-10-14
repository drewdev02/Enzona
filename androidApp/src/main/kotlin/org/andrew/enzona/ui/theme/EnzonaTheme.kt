package org.andrew.enzona.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta aproximada del mockup. Ajustar aquí cuando haya guía de marca propia. */
object EnzonaColors {
    val BlueLight = Color(0xFF1E96D6)
    val Blue = Color(0xFF2196D8)
    val Navy = Color(0xFF0F2A5E)
}

private val LightScheme = lightColorScheme(
    primary = EnzonaColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EBF8),
    onPrimaryContainer = EnzonaColors.Navy,
    secondary = EnzonaColors.Navy,
    // Blanco puro: el fondo por defecto de Material 3 (#FFFBFE) deja una franja y una costura visibles.
    background = Color.White,
    surface = Color.White,
)

@Composable
fun EnzonaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}
