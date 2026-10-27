package org.andrew.enzona.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.feature.auth.domain.SessionRepository

/** Estado de la pantalla de login. La sesión en sí (dentro/fuera) es de [SessionRepository]. */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Authenticating : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/**
 * ViewModel de la pantalla de login. La UI nativa abre la URL de authorize y devuelve el
 * `code`; el PKCE pendiente vive en el repositorio, así que recrear esta pantalla no lo pierde.
 */
class LoginViewModel(
    private val session: SessionRepository,
) : ViewModel() {
    private val log = logger<LoginViewModel>()

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Paso 1: devuelve la URL para abrir en el navegador nativo. */
    fun buildAuthorizeUrl(): String {
        val url = session.startLogin()
        _state.value = LoginUiState.Authenticating
        return url
    }

    /** Paso 2: la UI nativa entrega el `code` capturado del redirect apk-callback. */
    fun onRedirectCode(code: String) {
        viewModelScope.launch {
            _state.value = when (val r = session.completeLogin(code)) {
                // La sesión ya cambió a LoggedIn; esta pantalla desaparece, así que vuelve a Idle.
                is EzResult.Success -> LoginUiState.Idle
                is EzResult.Error -> LoginUiState.Error(r.failure.userMessage())
            }
        }
    }

    /** La UI nativa cancela o falla el login. */
    fun onAuthFailed(message: String) {
        // El texto lo aporta el navegador/IdP (p. ej. `oauthErrorMsg`): solo en Debug.
        log.debug("Motivo del fallo en el navegador: {Motivo}", message)
        session.cancelLogin()
        _state.value = LoginUiState.Error(message)
    }
}
