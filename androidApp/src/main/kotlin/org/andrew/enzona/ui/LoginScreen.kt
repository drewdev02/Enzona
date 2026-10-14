package org.andrew.enzona.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.andrew.enzona.feature.auth.presentation.AuthUiState

@Composable
fun LoginScreen(state: AuthUiState, onLogin: () -> Unit) {
    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("EnZona", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Inicia sesión con tu cuenta",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )

                when (state) {
                    is AuthUiState.Authenticating -> CircularProgressIndicator()
                    else -> Button(onClick = onLogin) { Text("Iniciar sesión") }
                }

                if (state is AuthUiState.Error) {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
