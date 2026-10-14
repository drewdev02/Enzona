package org.andrew.enzona.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.cards.presentation.CardsUiState
import org.andrew.enzona.feature.cards.presentation.CardsViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzEmptyState
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzLoadingState

/** Conecta el ViewModel con la pantalla. La pantalla en sí no sabe nada de Koin. */
@Composable
fun BalanceRoute(vm: CardsViewModel, onBack: () -> Unit, onCardMenu: () -> Unit) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    BalanceScreen(state, onBack, onRetry = vm::load, onCardMenu = onCardMenu)
}

/** Pantalla "Balance": las tarjetas del usuario, cada una con el aspecto de su banco. */
@Composable
fun BalanceScreen(
    state: CardsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCardMenu: () -> Unit,
) {
    Scaffold(
        topBar = { EzGradientTopAppBar("Balance", onBack) },
        containerColor = EnzonaTheme.colors.surfaceVariant,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is CardsUiState.Loading -> EzLoadingState()
                is CardsUiState.Error -> EzErrorState(state.message, onRetry)
                is CardsUiState.Empty -> EzEmptyState(
                    title = "No tienes tarjetas",
                    message = "Cuando agregues una tarjeta, aparecerá aquí con su saldo.",
                )
                is CardsUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // El Scaffold ya deja libre la barra de navegación del sistema.
                    contentPadding = PaddingValues(vertical = EnzonaTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(state.cards, key = { it.id }) { card ->
                        BankCardItem(card, onMenuClick = onCardMenu, modifier = Modifier.fillMaxWidth(0.95f))
                    }
                }
            }
        }
    }
}

// ---- Previews (datos inventados; no tocan Koin ni la red) ----

@Preview(name = "Balance: todos los skins", showBackground = true, widthDp = 360, heightDp = 2000)
@Composable
private fun BalanceAllSkinsPreview() = EnzonaTheme(darkTheme = false) {
    BalanceScreen(CardsUiState.Content(CardPreviewData.all), {}, {}, {})
}

@Preview(name = "Balance (oscuro)", showBackground = true, widthDp = 360, heightDp = 1300)
@Composable
private fun BalanceDarkPreview() = EnzonaTheme(darkTheme = true) {
    BalanceScreen(CardsUiState.Content(CardPreviewData.all.take(3)), {}, {}, {})
}

@Preview(name = "Balance: cargando", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun BalanceLoadingPreview() = EnzonaTheme(darkTheme = false) {
    BalanceScreen(CardsUiState.Loading, {}, {}, {})
}

@Preview(name = "Balance: vacío", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun BalanceEmptyPreview() = EnzonaTheme(darkTheme = false) {
    BalanceScreen(CardsUiState.Empty, {}, {}, {})
}

@Preview(name = "Balance: error", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun BalanceErrorPreview() = EnzonaTheme(darkTheme = false) {
    BalanceScreen(CardsUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo."), {}, {}, {})
}
