package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.andrew.enzona.ui.designsystem.EnzonaTheme

enum class EzAmountTone { Neutral, Positive, Negative }

/**
 * Importe con moneda. `value` llega ya formateado como String: el backend devuelve los montos
 * como texto y el dinero nunca se pasa por Double. El color refuerza el signo, no lo sustituye.
 */
@Composable
fun EzAmount(
    value: String,
    modifier: Modifier = Modifier,
    currency: String? = null,
    tone: EzAmountTone = EzAmountTone.Neutral,
    large: Boolean = false,
) {
    val c = EnzonaTheme.colors
    val color = when (tone) {
        EzAmountTone.Neutral -> c.onSurface
        EzAmountTone.Positive -> c.success
        EzAmountTone.Negative -> c.error
    }
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            value, color = color,
            style = if (large) EnzonaTheme.extraTypography.amountLarge else EnzonaTheme.extraTypography.amount,
        )
        currency?.let {
            Text(
                it, color = c.onSurfaceVariant, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = EnzonaTheme.spacing.xs, bottom = EnzonaTheme.spacing.xs),
            )
        }
    }
}
