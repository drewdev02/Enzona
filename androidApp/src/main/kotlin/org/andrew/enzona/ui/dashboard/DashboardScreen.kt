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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.presentation.AccountUiState
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.ui.components.BrandBadge
import org.andrew.enzona.ui.components.ReceiveGlyph
import org.andrew.enzona.ui.components.ScanGlyph
import org.andrew.enzona.ui.components.WhiteStatusBarIcons
import org.andrew.enzona.ui.theme.EnzonaColors
import org.andrew.enzona.ui.theme.EnzonaTheme

/** Conecta el ViewModel con la pantalla. La pantalla en sí no sabe nada de Koin. */
@Composable
fun DashboardRoute(vm: AccountViewModel, onScan: () -> Unit, onReceive: () -> Unit) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    DashboardScreen(state, onScan, onReceive, onRetry = vm::load)
}

private val ActionsHeight = 172.dp
private val BadgeSize = 80.dp

@Composable
fun DashboardScreen(
    state: AccountUiState,
    onScan: () -> Unit,
    onReceive: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteStatusBarIcons()
    val statusBar = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    // La hoja blanca arranca bajo las acciones; la insignia queda centrada en su borde superior.
    val sheetTop = statusBar + 20.dp + ActionsHeight

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(EnzonaColors.BlueLight, EnzonaColors.Navy)))
    ) {
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HeaderAction("Escanear", onScan) { ScanGlyph(Modifier.size(76.dp)) }
            HeaderAction("Recibir", onReceive) { ReceiveGlyph(Modifier.size(76.dp)) }
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(top = sheetTop),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color = Color.White,
        ) {
            Box(Modifier.fillMaxSize().padding(top = BadgeSize / 2 + 8.dp, start = 24.dp, end = 24.dp)) {
                SheetContent(state, onRetry)
            }
        }

        BrandBadge(
            Modifier.align(Alignment.TopCenter).offset(y = sheetTop - BadgeSize / 2),
            diameter = BadgeSize,
        )
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    Column(
        Modifier
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        glyph()
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun BoxScope.SheetContent(state: AccountUiState, onRetry: () -> Unit) {
    when (state) {
        is AccountUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        is AccountUiState.Error -> Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Reintentar") }
        }
        is AccountUiState.Content -> Column(Modifier.fillMaxWidth()) {
            Text("Hola, ${state.account.fullName}", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tus tarjetas y movimientos aparecerán aquí.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
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
private fun DashboardLoadingPreview() = EnzonaTheme {
    DashboardScreen(AccountUiState.Loading, {}, {}, {})
}

@Preview(name = "Con contenido", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardContentPreview() = EnzonaTheme {
    DashboardScreen(AccountUiState.Content(sampleAccount), {}, {}, {})
}

@Preview(name = "Error", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardErrorPreview() = EnzonaTheme {
    DashboardScreen(AccountUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo."), {}, {}, {})
}
