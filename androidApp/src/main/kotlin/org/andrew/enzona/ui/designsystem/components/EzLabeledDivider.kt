package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/** Encabezado de sección centrado entre dos líneas ("—— Últimas operaciones ——"). */
@Composable
fun EzLabeledDivider(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = EnzonaTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EzDivider(Modifier.weight(1f))
        Text(
            title,
            modifier = Modifier.padding(horizontal = EnzonaTheme.spacing.md).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = EnzonaTheme.colors.onSurfaceVariant,
        )
        EzDivider(Modifier.weight(1f))
    }
}
