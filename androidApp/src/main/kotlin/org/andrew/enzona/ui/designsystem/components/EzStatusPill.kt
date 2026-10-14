package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.andrew.enzona.ui.designsystem.EnzonaTheme

enum class EzTone { Success, Warning, Error, Info, Neutral }

/** Pastilla de estado (p. ej. "Aceptada", "Pendiente", "Rechazada"). El color nunca es el único portador: lleva texto. */
@Composable
fun EzStatusPill(text: String, tone: EzTone, modifier: Modifier = Modifier) {
    val c = EnzonaTheme.colors
    val (container, content) = when (tone) {
        EzTone.Success -> c.successContainer to c.onSuccessContainer
        EzTone.Warning -> c.warningContainer to c.onWarningContainer
        EzTone.Error -> c.errorContainer to c.onErrorContainer
        EzTone.Info -> c.infoContainer to c.onInfoContainer
        EzTone.Neutral -> c.surfaceVariant to c.onSurfaceVariant
    }
    Surface(modifier, shape = EnzonaTheme.shapes.pill, color = container, contentColor = content) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = EnzonaTheme.spacing.md, vertical = EnzonaTheme.spacing.xs),
        )
    }
}
