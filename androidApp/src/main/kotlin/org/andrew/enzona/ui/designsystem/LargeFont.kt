package org.andrew.enzona.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * Con la fuente muy grande no caben dos columnas: un importe no puede truncarse ni partirse, así que las filas
 * se apilan. Un solo umbral para toda la app (también lo usa la rejilla de servicios).
 */
const val LargeFontScale = 1.3f

@Composable
fun isLargeFont(): Boolean = LocalDensity.current.fontScale >= LargeFontScale
