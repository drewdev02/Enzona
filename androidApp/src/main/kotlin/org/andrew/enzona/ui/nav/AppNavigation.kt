package org.andrew.enzona.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.ui.HomeScreen
import org.andrew.enzona.ui.ProfileScreen

// Rutas tipadas (Navigation 2.8+): sin strings sueltas, los argumentos futuros serán campos.
@Serializable data object Home
@Serializable data object Profile

/** Navegación de la zona autenticada. El paso login <-> app lo decide AuthUiState, no este grafo. */
@Composable
fun AppNavigation(accountVm: AccountViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Home) {
        composable<Home> {
            HomeScreen(
                vm = accountVm,
                onOpenProfile = { nav.navigate(Profile) },
                onLogout = onLogout,
            )
        }
        composable<Profile> {
            ProfileScreen(vm = accountVm, onBack = { nav.popBackStack() })
        }
    }
}
