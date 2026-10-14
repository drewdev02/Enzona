package org.andrew.enzona.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.andrew.enzona.feature.auth.presentation.LoginUiState
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton

@Composable
fun LoginScreen(state: LoginUiState, sessionExpired: Boolean, onLogin: () -> Unit) {
    val spacing = EnzonaTheme.spacing
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("EnZona", style = MaterialTheme.typography.headlineLarge, color = EnzonaTheme.colors.onSurface)
            Text(
                "Inicia sesión con tu cuenta",
                style = MaterialTheme.typography.bodyMedium,
                color = EnzonaTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xs, bottom = spacing.xl),
            )
            EzButton("Iniciar sesión", onClick = onLogin, loading = state is LoginUiState.Authenticating)
            // Un error del intento actual tiene prioridad sobre el aviso de sesión caducada.
            val notice = when {
                state is LoginUiState.Error -> state.message
                sessionExpired && state is LoginUiState.Idle -> "Tu sesión expiró. Inicia sesión de nuevo."
                else -> null
            }
            if (notice != null) {
                Text(
                    notice,
                    color = EnzonaTheme.colors.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = spacing.lg),
                )
            }
        }
    }
}

@Preview(name = "Inicial", showBackground = true)
@Composable
private fun LoginPreview() = EnzonaTheme(darkTheme = false) { LoginScreen(LoginUiState.Idle, sessionExpired = false) {} }

@Preview(name = "Autenticando", showBackground = true)
@Composable
private fun LoginLoadingPreview() = EnzonaTheme(darkTheme = false) {
    LoginScreen(LoginUiState.Authenticating, sessionExpired = false) {}
}

@Preview(name = "Sesión caducada (oscuro)", showBackground = true)
@Composable
private fun LoginExpiredDarkPreview() = EnzonaTheme(darkTheme = true) {
    LoginScreen(LoginUiState.Idle, sessionExpired = true) {}
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun LoginErrorPreview() = EnzonaTheme(darkTheme = false) {
    LoginScreen(LoginUiState.Error("Login cancelado"), sessionExpired = false) {}
}
