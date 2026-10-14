package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/**
 * Fila de lista: [leading] · textos · [trailing]. Sirve para movimientos, tarjetas y ajustes.
 * `overline` es una etiqueta pequeña sobre el título (útil en filas "campo: valor").
 */
@Composable
fun EzListItem(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val spacing = EnzonaTheme.spacing
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = EnzonaTheme.sizes.listItemMinHeight)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            overline?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = EnzonaTheme.colors.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.bodyLarge, color = EnzonaTheme.colors.onSurface)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}
