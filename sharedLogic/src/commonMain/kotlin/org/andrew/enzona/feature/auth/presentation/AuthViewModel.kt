package org.andrew.enzona.feature.auth.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.crypto.Pkce
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.auth.domain.AuthRepository

/** Estado de autenticación que la UI nativa observa. */
sealed interface AuthUiState {
    data object LoggedOut : AuthUiState
    data object Authenticating : AuthUiState
    data object LoggedIn : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * ViewModel de auth compartido. Expone StateFlow para Compose (collectAsState) y
 * SwiftUI (vía SKIE). La UI nativa abre la URL de authorize y devuelve el `code`.
 */
class AuthViewModel(
    private val repo: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.LoggedOut)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var pendingPkce: Pkce? = null

    init {
        // Si el refresh_token deja de servir, volvemos al login con un aviso.
        scope.launch {
            repo.sessionExpired.collect {
                _state.value = AuthUiState.Error("Tu sesión expiró. Inicia sesión de nuevo.")
            }
        }
    }

    /** Restaura sesión guardada al arrancar. */
    fun restore() {
        if (repo.isLoggedIn()) _state.value = AuthUiState.LoggedIn
    }

    /** Paso 1: genera PKCE y devuelve la URL para abrir en el navegador nativo. */
    fun buildAuthorizeUrl(): String {
        val (pkce, url) = repo.startLogin()
        pendingPkce = pkce
        _state.value = AuthUiState.Authenticating
        return url
    }

    /** Paso 2: la UI nativa entrega el `code` capturado del redirect apk-callback. */
    fun onRedirectCode(code: String) {
        val pkce = pendingPkce ?: run {
            _state.value = AuthUiState.Error("Sin PKCE pendiente")
            return
        }
        scope.launch {
            when (val r = repo.completeLogin(code, pkce)) {
                is EzResult.Success -> _state.value = AuthUiState.LoggedIn
                is EzResult.Error -> _state.value =
                    AuthUiState.Error(r.failure.message ?: "Error de autenticación")
            }
            pendingPkce = null
        }
    }

    /** La UI nativa cancela o falla el login. */
    fun onAuthFailed(message: String) {
        pendingPkce = null
        _state.value = AuthUiState.Error(message)
    }

    fun logout() {
        repo.logout()
        _state.value = AuthUiState.LoggedOut
    }

    /** Estado actual (útil para Swift/UIKit en el primer render). */
    fun currentState(): AuthUiState = _state.value

    /** Observa el estado desde Swift; devuelve una suscripción cancelable. */
    fun watchState(onEach: (AuthUiState) -> Unit): Cancellable = state.watch(onEach)
}
