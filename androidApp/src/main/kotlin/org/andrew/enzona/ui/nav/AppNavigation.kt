package org.andrew.enzona.ui.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.ui.ComingSoonScreen
import org.andrew.enzona.ui.ProfileScreen
import org.andrew.enzona.ui.dashboard.DashboardRoute

// Rutas tipadas (Navigation 2.8+): sin strings sueltas, los argumentos futuros serán campos.
@Serializable data object Home
@Serializable data object Notifications
@Serializable data object Friends
@Serializable data object Profile

/** Navegación de la zona autenticada. El paso login <-> app lo decide AuthUiState, no este grafo. */
@Composable
fun AppNavigation(accountVm: AccountViewModel, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val selected = TopLevel.entries.firstOrNull { entry?.destination?.hasRoute(it.route::class) == true }
        ?: TopLevel.HOME

    Scaffold(
        // Cada pantalla gestiona sus propios insets (el dashboard dibuja bajo la barra de estado).
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            EnzonaBottomBar(selected = selected, onSelect = { dest ->
                nav.navigate(dest.route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Home,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
        ) {
            composable<Home> {
                // Escanear / Recibir llegan con el hito de QR (M5).
                DashboardRoute(
                    onScan = {}, onReceive = {},
                    onServiceClick = { service ->
                        // La navegación de cada servicio se añade con su hito; hasta entonces avisa.
                        if (!service.isAvailable) scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            snackbar.showSnackbar("${service.label} estará disponible próximamente.")
                        }
                    },
                )
            }
            composable<Notifications> {
                ComingSoonScreen("Avisos", "Las notificaciones llegarán más adelante.")
            }
            composable<Friends> {
                ComingSoonScreen("Amigos", "Contactos y destinatarios llegarán más adelante.")
            }
            composable<Profile> { ProfileScreen(accountVm, onLogout) }
        }
    }
}
