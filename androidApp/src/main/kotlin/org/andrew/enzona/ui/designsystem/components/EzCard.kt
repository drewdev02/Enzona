package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/** Contenedor de contenido agrupado. Con `onClick` es pulsable (con ripple y semántica de botón). */
@Composable
fun EzCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = EnzonaTheme.spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = EnzonaTheme.colors.surfaceVariant,
        contentColor = EnzonaTheme.colors.onSurface,
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = EnzonaTheme.elevation.none)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Card(onClick, modifier, shape = EnzonaTheme.shapes.lg, colors = colors, elevation = elevation, content = inner)
    } else {
        Card(modifier, shape = EnzonaTheme.shapes.lg, colors = colors, elevation = elevation, content = inner)
    }
}
