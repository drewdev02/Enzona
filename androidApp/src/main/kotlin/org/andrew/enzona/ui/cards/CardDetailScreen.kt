package org.andrew.enzona.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardOperation
import org.andrew.enzona.feature.cards.presentation.CardDetailUiState
import org.andrew.enzona.feature.cards.presentation.CardDetailViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzAmount
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzLabeledDivider
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Conecta el ViewModel (uno por pantalla, con el id de la tarjeta) con la pantalla. */
@Composable
fun CardDetailRoute(
    cardId: String,
    cardLabel: String,
    onBack: () -> Unit,
    vm: CardDetailViewModel = koinViewModel(parameters = { parametersOf(cardId) }),
) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    CardDetailScreen(state, cardLabel, onBack, onRetry = vm::load)
}

/** Detalle de una tarjeta: saldos y últimas operaciones. Sin ViewModel: se puede previsualizar. */
@Composable
fun CardDetailScreen(
    state: CardDetailUiState,
    cardLabel: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = { EzGradientTopAppBar("Balance", onBack) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is CardDetailUiState.Loading -> EzLoadingState()
                is CardDetailUiState.Error -> EzErrorState(state.message, onRetry)
                is CardDetailUiState.Content -> ActivityContent(state.activity, cardLabel)
            }
        }
    }
}

@Composable
private fun ActivityContent(activity: CardActivity, cardLabel: String) {
    val spacing = EnzonaTheme.spacing
    LazyColumn(Modifier.fillMaxSize()) {
        item { AvailableBalance(activity.available) }
        item { DetailRow("Saldo contable", activity.balance?.let(::moneyWithSymbol) ?: "—") }
        item { EzDivider() }
        item { DetailRow("Tarjeta", cardLabel) }
        item { EzLabeledDivider("Últimas operaciones") }
        if (activity.operations.isEmpty()) {
            item {
                Text(
                    "Sin operaciones recientes",
                    modifier = Modifier.fillMaxWidth().padding(spacing.xl),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = EnzonaTheme.colors.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(activity.operations) { _, operation -> OperationRow(operation) }
        }
        if (activity.skippedOperations > 0) {
            item {
                // No ocultamos que faltan datos: la lista puede estar incompleta.
                Text(
                    "No se pudieron mostrar ${activity.skippedOperations} operaciones.",
                    modifier = Modifier.fillMaxWidth().padding(spacing.lg),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EnzonaTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** El original usa peso regular en las cabeceras de saldo y en las filas de datos. */
private fun Regular(style: TextStyle): TextStyle = style.copy(fontWeight = FontWeight.Normal)

/** `$3395.15`: el símbolo y el formato son los del original; no hay separador de miles. */
private fun moneyWithSymbol(money: Money): String = "$" + formatMinorUnits(money.minorUnits) + " " + displayCurrency(money.currency)

@Composable
private fun AvailableBalance(available: Money?) {
    val spacing = EnzonaTheme.spacing
    val description = "Saldo disponible " + (available?.let(::moneyWithSymbol) ?: "no disponible")
    Column(
        Modifier.fillMaxWidth().padding(top = spacing.lg, bottom = spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Saldo disponible", style = Regular(MaterialTheme.typography.titleLarge), color = EnzonaTheme.colors.onSurfaceVariant)
        if (available != null) {
            EzAmount("$" + formatMinorUnits(available.minorUnits), currency = displayCurrency(available.currency), large = true)
        } else {
            Text("—", style = EnzonaTheme.extraTypography.amountLarge, color = EnzonaTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val spacing = EnzonaTheme.spacing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Regular(MaterialTheme.typography.titleLarge), color = EnzonaTheme.colors.onSurface)
        Text(value, style = Regular(MaterialTheme.typography.titleLarge), color = EnzonaTheme.colors.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = spacing.md))
    }
}

@Composable
private fun OperationRow(operation: CardOperation) {
    val spacing = EnzonaTheme.spacing
    val title = operation.kind.title(operation.merchantAlias)
    val currency = displayCurrency(operation.amount.currency)
    // El importe llega como magnitud; el signo sale del sentido de la operación.
    val amount = operation.direction.sign() + formatMinorUnits(operation.amount.minorUnits).removePrefix("-")
    val spoken = listOfNotNull(
        title, operation.direction.label(), "$amount $currency", operation.dateText.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Image(painterResource(operation.kind.iconRes), contentDescription = null, Modifier.size(EnzonaTheme.sizes.avatarLg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = EnzonaTheme.colors.onSurface, maxLines = 2)
            Text(operation.direction.label(), style = MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            EzAmount(amount, currency = currency)
            if (operation.dateText.isNotBlank()) {
                Text(operation.dateText, style = MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant)
            }
        }
    }
}

// ---- Previews (datos inventados; no tocan Koin ni la red) ----

private const val Label = "BANMET 6856"

@Preview(name = "Detalle", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CardDetailPreview() = EnzonaTheme(darkTheme = false) {
    CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.activity), Label, {}, {})
}

@Preview(name = "Detalle (oscuro)", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CardDetailDarkPreview() = EnzonaTheme(darkTheme = true) {
    CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.activity), Label, {}, {})
}

@Preview(name = "Detalle (fuente grande)", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.6f)
@Composable
private fun CardDetailLargeFontPreview() = EnzonaTheme(darkTheme = false) {
    CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.activity), Label, {}, {})
}

@Preview(name = "Detalle: sin operaciones", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CardDetailNoOperationsPreview() = EnzonaTheme(darkTheme = false) {
    CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.noOperations), Label, {}, {})
}

@Preview(name = "Detalle: datos parciales", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CardDetailPartialPreview() = EnzonaTheme(darkTheme = false) {
    CardDetailScreen(CardDetailUiState.Content(CardDetailPreviewData.partial), Label, {}, {})
}

@Preview(name = "Detalle: cargando", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CardDetailLoadingPreview() = EnzonaTheme(darkTheme = false) {
    CardDetailScreen(CardDetailUiState.Loading, Label, {}, {})
}

@Preview(name = "Detalle: error", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CardDetailErrorPreview() = EnzonaTheme(darkTheme = false) {
    CardDetailScreen(CardDetailUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo."), Label, {}, {})
}
