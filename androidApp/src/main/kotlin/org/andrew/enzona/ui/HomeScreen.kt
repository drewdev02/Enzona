package org.andrew.enzona.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.account.presentation.AccountUiState
import org.andrew.enzona.feature.account.presentation.AccountViewModel

@Composable
fun HomeScreen(vm: AccountViewModel, onOpenProfile: () -> Unit, onLogout: () -> Unit) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()

    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val greeting = (state as? AccountUiState.Content)?.account?.fullName
                Text(
                    if (greeting != null) "Hola, $greeting" else "Sesión iniciada",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Tarjetas y movimientos llegan en M3 y M4",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Button(onClick = onOpenProfile) { Text("Mi perfil") }
                OutlinedButton(onClick = onLogout, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}
