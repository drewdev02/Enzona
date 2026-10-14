package org.andrew.enzona.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.presentation.AccountUiState
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.WhiteStatusBarIcons
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.andrew.enzona.ui.designsystem.icons.BrandBadge
import org.andrew.enzona.ui.designsystem.icons.ReceiveGlyph
import org.andrew.enzona.ui.designsystem.icons.ScanGlyph

/** Conecta el ViewModel con la pantalla. La pantalla en sí no sabe nada de Koin. */
@Composable
fun DashboardRoute(vm: AccountViewModel, onScan: () -> Unit, onReceive: () -> Unit) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    DashboardScreen(state, onScan, onReceive, onRetry = vm::load)
}

@Composable
fun DashboardScreen(
    state: AccountUiState,
    onScan: () -> Unit,
    onReceive: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteStatusBarIcons()
    val colors = EnzonaTheme.colors
    val spacing = EnzonaTheme.spacing
    val sizes = EnzonaTheme.sizes
    val statusBar = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    // La hoja arranca bajo las acciones; la insignia queda centrada en su borde superior.
    val sheetTop = statusBar + spacing.xl + sizes.heroActionsHeight

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(colors.brandBlueLight, colors.brandNavy)))
    ) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = spacing.xl)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HeaderAction("Escanear", onScan) { ScanGlyph(Modifier.size(sizes.heroGlyph)) }
            HeaderAction("Recibir", onReceive) { ReceiveGlyph(Modifier.size(sizes.heroGlyph)) }
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(top = sheetTop),
            shape = EnzonaTheme.shapes.sheet,
            color = colors.surface,
        ) {
            Box(
                Modifier.fillMaxSize().padding(
                    top = sizes.brandBadge / 2 + spacing.sm, start = spacing.xl, end = spacing.xl,
                )
            ) { SheetContent(state, onRetry) }
        }

        BrandBadge(
            Modifier.align(Alignment.TopCenter).offset(y = sheetTop - sizes.brandBadge / 2),
            diameter = sizes.brandBadge,
        )
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    Column(
        Modifier
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(EnzonaTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        glyph()
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = EnzonaTheme.spacing.sm))
    }
}

@Composable
private fun BoxScope.SheetContent(state: AccountUiState, onRetry: () -> Unit) {
    when (state) {
        is AccountUiState.Loading -> EzLoadingState()
        is AccountUiState.Error -> EzErrorState(state.message, onRetry)
        is AccountUiState.Content -> Column(Modifier.fillMaxWidth()) {
            Text("Hola, ${state.account.fullName}", style = MaterialTheme.typography.titleLarge,
                color = EnzonaTheme.colors.onSurface)
            Text(
                "Tus tarjetas y movimientos aparecerán aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = EnzonaTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = EnzonaTheme.spacing.sm),
            )
        }
    }
}

// ---- Previews (con datos inventados; no tocan Koin ni la red) ----

private val sampleAccount = Account(
    uuid = "sample", username = "user1", firstName = "Test", lastName = "User",
    email = "test@example.com", phone = "55555555", avatarUrl = null, primaryCurrency = "CUP",
    profileLevel = 8, isVerified = true, isPhoneVerified = true, hasPaymentPassword = true,
    biometricEnabled = true, isMerchant = false, createdAt = null,
)

@Preview(name = "Cargando", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardLoadingPreview() = EnzonaTheme(darkTheme = false) {
    DashboardScreen(AccountUiState.Loading, {}, {}, {})
}

@Preview(name = "Con contenido", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardContentPreview() = EnzonaTheme(darkTheme = false) {
    DashboardScreen(AccountUiState.Content(sampleAccount), {}, {}, {})
}

@Preview(name = "Con contenido (oscuro)", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardContentDarkPreview() = EnzonaTheme(darkTheme = true) {
    DashboardScreen(AccountUiState.Content(sampleAccount), {}, {}, {})
}

@Preview(name = "Error", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardErrorPreview() = EnzonaTheme(darkTheme = false) {
    DashboardScreen(AccountUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo."), {}, {}, {})
}
