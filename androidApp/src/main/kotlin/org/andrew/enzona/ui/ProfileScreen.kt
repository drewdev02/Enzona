package org.andrew.enzona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.presentation.AccountUiState
import org.andrew.enzona.feature.account.presentation.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AccountViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mi perfil") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (val s = state) {
                    is AccountUiState.Loading ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is AccountUiState.Error -> ErrorBlock(s.message, onRetry = vm::load)
                    is AccountUiState.Content -> ProfileContent(s.account)
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(account: Account) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Iniciales: la foto remota (avatarUrl) llega cuando se añada una librería de imágenes.
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(account.initials, style = MaterialTheme.typography.headlineMedium)
            }
            Text(account.fullName, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp))
            Text("@${account.username}", style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()
        Row("Correo", account.email)
        Row("Teléfono", account.phone)
        Row("Moneda principal", account.primaryCurrency)
        Row("Nivel de perfil", account.profileLevel?.toString())
        HorizontalDivider()
        Row("Cuenta verificada", yesNo(account.isVerified))
        Row("Teléfono verificado", yesNo(account.isPhoneVerified))
        Row("Contraseña de pago", if (account.hasPaymentPassword) "Configurada" else "Sin configurar")
        Row("Biometría", yesNo(account.biometricEnabled))
        Row("Cuenta de comercio", yesNo(account.isMerchant))
    }
}

private fun yesNo(value: Boolean) = if (value) "Sí" else "No"

@Composable
private fun Row(label: String, value: String?) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = { Text(value ?: "—") },
    )
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Reintentar") }
    }
}
