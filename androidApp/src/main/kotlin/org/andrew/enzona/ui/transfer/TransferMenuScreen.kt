package org.andrew.enzona.ui.transfer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.annotation.DrawableRes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.R
import org.andrew.enzona.feature.transfer.presentation.TransferMenuUiState
import org.andrew.enzona.feature.transfer.presentation.TransferMenuViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzLabeledDivider
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.koin.compose.viewmodel.koinViewModel

/** Las cuatro formas de transferir del menú, en el orden del original. */
enum class TransferOption(val label: String, @DrawableRes val iconRes: Int) {
    FRIEND("A un amigo", R.drawable.transfer_option_friend),
    ACCOUNT("A una cuenta", R.drawable.transfer_option_account),
    CARD("A una tarjeta", R.drawable.transfer_option_card),
    OWN_CARD("A una de mis tarjetas", R.drawable.transfer_option_own_card),
}

@Composable
fun TransferMenuRoute(
    onBack: () -> Unit,
    onOption: (TransferOption) -> Unit,
    onHistory: () -> Unit,
    vm: TransferMenuViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    TransferMenuScreen(state, onBack, onOption, onHistory, onRetry = vm::load)
}

/** Menú "Transferir": formas de transferir y las últimas transferencias. Sin ViewModel: se puede previsualizar. */
@Composable
fun TransferMenuScreen(
    state: TransferMenuUiState,
    onBack: () -> Unit,
    onOption: (TransferOption) -> Unit,
    onHistory: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = { EzGradientTopAppBar("Transferir", onBack, actionText = "Historial", onAction = onHistory) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(TransferOption.entries) { option ->
                EzListItem(
                    title = option.label,
                    leading = { Image(painterResource(option.iconRes), null, Modifier.size(EnzonaTheme.sizes.avatarLg)) },
                    onClick = { onOption(option) },
                )
                EzDivider()
            }
            item { EzLabeledDivider("Recientes") }
            when (state) {
                is TransferMenuUiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(EnzonaTheme.spacing.xl)) { EzLoadingState() } }
                is TransferMenuUiState.Error -> item { EzErrorState(state.message, onRetry) }
                is TransferMenuUiState.Content -> {
                    if (state.recent.isEmpty()) {
                        item { Message("Aún no has hecho transferencias") }
                    } else {
                        items(state.recent, key = { it.id }) { TransferRow(it) }
                    }
                    if (state.skipped > 0) item { Message("No se pudieron mostrar ${state.skipped} transferencias.") }
                }
            }
        }
    }
}

@Composable
internal fun Message(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(EnzonaTheme.spacing.xl),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = EnzonaTheme.colors.onSurfaceVariant,
    )
}

@Preview(name = "Menú", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TransferMenuPreview() = EnzonaTheme(darkTheme = false) {
    TransferMenuScreen(TransferMenuUiState.Content(TransferPreviewData.transfers.take(4), 0), {}, {}, {}, {})
}

@Preview(name = "Menú (fuente grande)", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.6f)
@Composable
private fun TransferMenuLargeFontPreview() = EnzonaTheme(darkTheme = false) {
    TransferMenuScreen(TransferMenuUiState.Content(TransferPreviewData.transfers.take(3), 0), {}, {}, {}, {})
}
