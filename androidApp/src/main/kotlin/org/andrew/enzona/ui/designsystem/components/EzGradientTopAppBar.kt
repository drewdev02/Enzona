package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.WhiteStatusBarIcons

/**
 * Barra superior con el degradado de la marca, para pantallas de segundo nivel. Texto e iconos
 * blancos (el degradado es siempre oscuro, también en tema claro) y barra de estado clara.
 */
@Composable
fun EzGradientTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Texto de una acción a la derecha ("Historial"). Va con [onAction]. */
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    WhiteStatusBarIcons()
    val colors = EnzonaTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .shadow(EnzonaTheme.elevation.medium)
            .background(Brush.horizontalGradient(listOf(colors.brandBlueLight, colors.brandNavy)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(EnzonaTheme.sizes.minTouchTarget + EnzonaTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = EnzonaTheme.spacing.xs)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
        }
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = EnzonaTheme.spacing.sm).semantics { heading() },
        )
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.padding(end = EnzonaTheme.spacing.xs)) {
                Text(actionText, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
