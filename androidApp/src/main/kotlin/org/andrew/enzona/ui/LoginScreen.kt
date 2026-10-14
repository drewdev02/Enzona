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
import org.andrew.enzona.feature.auth.presentation.AuthUiState
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton

@Composable
fun LoginScreen(state: AuthUiState, onLogin: () -> Unit) {
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
            EzButton("Iniciar sesión", onClick = onLogin, loading = state is AuthUiState.Authenticating)
            if (state is AuthUiState.Error) {
                Text(
                    state.message,
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
private fun LoginPreview() = EnzonaTheme(darkTheme = false) { LoginScreen(AuthUiState.LoggedOut) {} }

@Preview(name = "Autenticando", showBackground = true)
@Composable
private fun LoginLoadingPreview() = EnzonaTheme(darkTheme = false) { LoginScreen(AuthUiState.Authenticating) {} }

@Preview(name = "Error (oscuro)", showBackground = true)
@Composable
private fun LoginErrorDarkPreview() = EnzonaTheme(darkTheme = true) {
    LoginScreen(AuthUiState.Error("Tu sesión expiró. Inicia sesión de nuevo.")) {}
}
