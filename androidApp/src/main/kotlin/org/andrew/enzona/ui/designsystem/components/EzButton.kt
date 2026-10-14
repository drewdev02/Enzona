package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.andrew.enzona.ui.designsystem.EnzonaTheme

enum class EzButtonStyle { Primary, Secondary, Tertiary, Destructive }

/**
 * Botón estándar. `loading` muestra un indicador y bloquea los toques, pero conserva el
 * aspecto activo (con `enabled = false` el botón se vería "apagado" durante la carga).
 */
@Composable
fun EzButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: EzButtonStyle = EzButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val sizes = EnzonaTheme.sizes
    val spacing = EnzonaTheme.spacing
    val colors = EnzonaTheme.colors
    val shape = EnzonaTheme.shapes.pill
    val m = modifier
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .heightIn(min = sizes.buttonHeight)
    val onTap = { if (!loading) onClick() }

    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(sizes.iconMd - spacing.xs),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        } else {
            leadingIcon?.let { it(); Spacer(Modifier.width(spacing.sm)) }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }

    when (style) {
        EzButtonStyle.Primary -> Button(onTap, m, enabled = enabled, shape = shape, content = content)
        EzButtonStyle.Secondary -> FilledTonalButton(onTap, m, enabled = enabled, shape = shape, content = content)
        EzButtonStyle.Tertiary -> TextButton(onTap, m, enabled = enabled, shape = shape, content = content)
        EzButtonStyle.Destructive -> Button(
            onClick = onTap, modifier = m, enabled = enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
            content = content,
        )
    }
}
