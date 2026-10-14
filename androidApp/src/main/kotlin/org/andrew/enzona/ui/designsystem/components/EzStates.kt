package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/** Los tres estados de cualquier pantalla con datos remotos: cargando, error y vacío. */
@Composable
fun EzLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EzErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(EnzonaTheme.spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = EnzonaTheme.colors.error, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge)
        EzButton(
            "Reintentar", onRetry,
            Modifier.padding(top = EnzonaTheme.spacing.lg),
            style = EzButtonStyle.Secondary, fillWidth = false,
        )
    }
}

@Composable
fun EzEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(EnzonaTheme.spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.invoke()
        Text(title, style = MaterialTheme.typography.headlineSmall, color = EnzonaTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (icon != null) EnzonaTheme.spacing.md else 0.dp))
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = EnzonaTheme.spacing.sm))
        }
    }
}
