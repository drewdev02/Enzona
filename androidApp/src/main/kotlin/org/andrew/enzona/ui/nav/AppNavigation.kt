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
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.andrew.enzona.feature.home.domain.HomeService
import org.andrew.enzona.ui.cards.BalanceRoute
import org.andrew.enzona.ui.cards.CardDetailRoute
import org.andrew.enzona.ui.cards.detailLabel
import org.andrew.enzona.ui.ComingSoonScreen
import org.andrew.enzona.ui.ProfileScreen
import org.andrew.enzona.ui.dashboard.DashboardRoute
import org.andrew.enzona.ui.transfer.FindAccountRoute
import org.andrew.enzona.ui.transfer.OwnCardTransferRoute
import org.andrew.enzona.ui.transfer.TransferFriendsRoute
import org.andrew.enzona.ui.transfer.TransferHistoryRoute
import org.andrew.enzona.ui.transfer.TransferMenuRoute
import org.andrew.enzona.ui.transfer.TransferOption

// Rutas tipadas (Navigation 2.8+): sin strings sueltas, los argumentos futuros serán campos.
@Serializable data object Home
@Serializable data object Notifications
@Serializable data object Friends
@Serializable data object Profile
/** Segundo nivel (no es una pestaña): se abre desde la casilla "Balance" del inicio. */
@Serializable data object Balance
/** Tercer nivel: saldo y operaciones de una tarjeta. `cardLabel` es el texto de la fila "Tarjeta" ("BANMET 6856"). */
@Serializable data class CardDetail(val cardId: String, val cardLabel: String)
/** Segundo nivel: menú "Transferir" (formas de transferir y recientes) y sus pantallas de apoyo. */
@Serializable data object TransferMenu
@Serializable data object TransferHistory
@Serializable data object TransferFindAccount
@Serializable data object TransferFriends
/** "A una de mis tarjetas": el primer formulario que mueve dinero. */
@Serializable data object TransferOwnCard

/** Navegación de la zona autenticada. El paso login <-> app lo decide SessionState, no este grafo. */
@Composable
fun AppNavigation(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val topLevel = TopLevel.entries.firstOrNull { entry?.destination?.hasRoute(it.route::class) == true }
    val selected = topLevel ?: TopLevel.HOME
    fun showMessage(text: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(text)
    }

    Scaffold(
        // Cada pantalla gestiona sus propios insets (el dashboard dibuja bajo la barra de estado).
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        // Las pantallas de segundo nivel (Balance) no llevan barra inferior, como en el original.
        bottomBar = {
            if (topLevel != null) EnzonaBottomBar(selected = selected, onSelect = { dest ->
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
                        when {
                            !service.isAvailable -> showMessage("${service.label} estará disponible próximamente.")
                            service == HomeService.BALANCE -> nav.navigate(Balance)
                            service == HomeService.TRANSFER -> nav.navigate(TransferMenu)
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
            composable<Profile> { ProfileScreen(onLogout) }
            composable<Balance> {
                BalanceRoute(
                    onBack = { nav.popBackStack() },
                    // Eliminar / predeterminada / PIN / operaciones son operaciones nuevas: llegan en su hito.
                    onCardMenu = { showMessage("Las acciones de la tarjeta estarán disponibles próximamente.") },
                    onCardClick = { card -> nav.navigate(CardDetail(card.id, card.detailLabel())) },
                )
            }
            composable<TransferMenu> {
                TransferMenuRoute(
                    onBack = { nav.popBackStack() },
                    onHistory = { nav.navigate(TransferHistory) },
                    onOption = { option ->
                        when (option) {
                            TransferOption.ACCOUNT -> nav.navigate(TransferFindAccount)
                            TransferOption.FRIEND -> nav.navigate(TransferFriends)
                            TransferOption.OWN_CARD -> nav.navigate(TransferOwnCard)
                            // Los demás formularios que mueven dinero llegan en su hito.
                            TransferOption.CARD ->
                                showMessage("${option.label} estará disponible próximamente.")
                        }
                    },
                )
            }
            composable<TransferHistory> { TransferHistoryRoute(onBack = { nav.popBackStack() }) }
            composable<TransferOwnCard> {
                OwnCardTransferRoute(onBack = { nav.popBackStack() }, onOpenHistory = { nav.navigate(TransferHistory) })
            }
            composable<TransferFindAccount> {
                FindAccountRoute(
                    onBack = { nav.popBackStack() },
                    onRecipient = { showMessage("Transferir a una cuenta estará disponible próximamente.") },
                )
            }
            composable<TransferFriends> {
                TransferFriendsRoute(
                    onBack = { nav.popBackStack() },
                    onFriend = { showMessage("Transferir a un amigo estará disponible próximamente.") },
                )
            }
            composable<CardDetail> { entry ->
                val args = entry.toRoute<CardDetail>()
                CardDetailRoute(args.cardId, args.cardLabel, onBack = { nav.popBackStack() })
            }
        }
    }
}
