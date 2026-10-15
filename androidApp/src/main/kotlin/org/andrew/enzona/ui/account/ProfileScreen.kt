package org.andrew.enzona.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.presentation.AccountUiState
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzAvatar
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzButtonStyle
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.andrew.enzona.ui.designsystem.components.EzTopAppBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(onLogout: () -> Unit, vm: AccountViewModel = koinViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    // La sección de depuración la inyecta la ruta (que ya usa Koin): las previews quedan puras.
    ProfileContentScreen(
        state, onRetry = vm::load, onLogout = onLogout,
        debugSection = { DebugTokenSection() },
    )
}

/** Sin ViewModel: se puede previsualizar. */
@Composable
fun ProfileContentScreen(
    state: AccountUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    debugSection: (@Composable () -> Unit)? = null,
) {
    Scaffold(topBar = { EzTopAppBar("Mi perfil") }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is AccountUiState.Loading -> EzLoadingState()
                is AccountUiState.Error -> EzErrorState(state.message, onRetry)
                is AccountUiState.Content -> ProfileContent(state.account, onLogout, debugSection)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    account: Account,
    onLogout: () -> Unit,
    debugSection: (@Composable () -> Unit)? = null,
) {
    val spacing = EnzonaTheme.spacing
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EzAvatar(account.initials, size = EnzonaTheme.sizes.avatarXl)
            Text(account.fullName, style = MaterialTheme.typography.headlineSmall,
                color = EnzonaTheme.colors.onSurface, modifier = Modifier.padding(top = spacing.md))
            Text("@${account.username}", style = MaterialTheme.typography.bodyMedium,
                color = EnzonaTheme.colors.onSurfaceVariant)
        }
        EzDivider()
        EzListItem(overline = "Correo", title = account.email ?: "—")
        EzListItem(overline = "Teléfono", title = account.phone ?: "—")
        EzListItem(overline = "Moneda principal", title = account.primaryCurrency ?: "—")
        EzListItem(overline = "Nivel de perfil", title = account.profileLevel?.toString() ?: "—")
        EzDivider()
        EzListItem(overline = "Cuenta verificada", title = yesNo(account.isVerified))
        EzListItem(overline = "Teléfono verificado", title = yesNo(account.isPhoneVerified))
        EzListItem(overline = "Contraseña de pago",
            title = if (account.hasPaymentPassword) "Configurada" else "Sin configurar")
        EzListItem(overline = "Biometría", title = yesNo(account.biometricEnabled))
        EzListItem(overline = "Cuenta de comercio", title = yesNo(account.isMerchant))
        EzButton(
            "Cerrar sesión", onLogout,
            Modifier.padding(spacing.xl),
            style = EzButtonStyle.Secondary,
        )
        debugSection?.invoke()
    }
}

private fun yesNo(value: Boolean) = if (value) "Sí" else "No"

private val sampleAccount = Account(
    uuid = "sample", username = "user1", firstName = "Test", lastName = "User",
    email = "test@example.com", phone = "55555555", avatarUrl = null, primaryCurrency = "CUP",
    profileLevel = 8, isVerified = true, isPhoneVerified = true, hasPaymentPassword = true,
    biometricEnabled = false, isMerchant = false, createdAt = null,
)

@Preview(name = "Perfil", showBackground = true, heightDp = 900)
@Composable
private fun ProfilePreview() = EnzonaTheme(darkTheme = false) {
    ProfileContentScreen(AccountUiState.Content(sampleAccount), {}, {})
}

@Preview(name = "Perfil (oscuro)", showBackground = true, heightDp = 900)
@Composable
private fun ProfileDarkPreview() = EnzonaTheme(darkTheme = true) {
    ProfileContentScreen(AccountUiState.Content(sampleAccount), {}, {})
}

@Preview(name = "Perfil: error", showBackground = true)
@Composable
private fun ProfileErrorPreview() = EnzonaTheme(darkTheme = false) {
    ProfileContentScreen(AccountUiState.Error("Sin conexión. Revisa tu red e inténtalo de nuevo."), {}, {})
}
