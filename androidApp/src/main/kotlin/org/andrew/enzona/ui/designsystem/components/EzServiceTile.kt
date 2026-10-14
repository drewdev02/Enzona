package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/**
 * Casilla de servicio: icono grande con la etiqueta debajo. La etiqueta es el nombre
 * accesible de todo el botón (el icono es decorativo, sin descripción propia).
 */
@Composable
fun EzServiceTile(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = EnzonaTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(icon, contentDescription = null, Modifier.size(EnzonaTheme.sizes.serviceIcon), contentScale = ContentScale.Fit)
        // Tamaños normales: una línea que se reduce hasta 10sp antes de cortar una palabra
        // ("Organizaciones"). Con fuente grande no se reduce y puede ocupar varias líneas (la
        // cuadrícula pasa a 2 columnas, donde las palabras caben).
        val largeText = isLargeText()
        Text(
            label,
            style = EnzonaTheme.extraTypography.tileLabel,
            color = EnzonaTheme.colors.onSurface,
            maxLines = if (largeText) 3 else 1,
            autoSize = if (largeText) null else TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.sp),
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Fuente del usuario >= 1.3x: la cuadrícula necesita otra disposición. */
@Composable
private fun isLargeText(): Boolean = LocalDensity.current.fontScale >= 1.3f

/** Cuadrícula de casillas (4 columnas; 2 con fuente grande). Se desplaza si no cabe. */
@Composable
fun <T> EzServiceGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    contentPadding: PaddingValues = PaddingValues(),
    tile: @Composable (T) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLargeText()) 2 else columns),
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.xs),
    ) {
        items(items) { tile(it) }
    }
}
