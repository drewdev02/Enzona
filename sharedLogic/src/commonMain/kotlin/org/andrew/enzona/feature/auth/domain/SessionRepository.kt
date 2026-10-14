package org.andrew.enzona.feature.auth.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.andrew.enzona.core.crypto.Pkce
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.result.map
import org.andrew.enzona.feature.auth.data.JwtClaims
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore

/** Estado de la sesión del usuario. Es un hecho del dominio: sin textos ni estado de pantalla. */
sealed interface SessionState {
    /** [expired]: la sesión se perdió porque el servidor rechazó el refresh_token (no por acción del usuario). */
    data class LoggedOut(val expired: Boolean = false) : SessionState
    data object LoggedIn : SessionState
}

/**
 * Dueño de la sesión durante toda la vida de la app: tokens, estado y el PKCE del login en curso.
 * Las pantallas (y sus ViewModels) observan [state]; ninguna es dueña de la sesión, así que
 * recrearlas no pierde un login a medias ni deja datos del usuario anterior.
 */
class SessionRepository(
    private val oauth: OAuthService,
    private val tokenStore: TokenStore,
) {
    private val _state = MutableStateFlow<SessionState>(
        if (tokenStore.hasSession()) SessionState.LoggedIn else SessionState.LoggedOut(),
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var pendingPkce: Pkce? = null

    /** Genera el PKCE, lo guarda para canjear el code después y devuelve la URL de authorize. */
    fun startLogin(): String {
        val pkce = Pkce.generate()
        pendingPkce = pkce
        return oauth.authorizeUrl(pkce)
    }

    /** El usuario canceló o el navegador falló: descarta el PKCE pendiente. */
    fun cancelLogin() {
        pendingPkce = null
    }

    /** Canjea el code capturado del redirect y abre la sesión. */
    suspend fun completeLogin(code: String): EzResult<Unit> {
        val pkce = pendingPkce ?: return EzResult.Error(Failure.Auth("No hay un inicio de sesión en curso."))
        val result = oauth.exchangeCode(code, pkce)
        pendingPkce = null
        if (result is EzResult.Success) {
            tokenStore.save(result.value)
            _state.value = SessionState.LoggedIn
        }
        return result.map { }
    }

    /**
     * Renueva el access_token con el refresh_token guardado.
     * Si el servidor lo rechaza (Failure.Auth) la sesión se da por perdida: se borran los
     * tokens y el estado pasa a `LoggedOut(expired = true)`. Un fallo de red NO cierra la sesión.
     */
    suspend fun refresh(): EzResult<Unit> {
        val rt = tokenStore.refreshToken()
        if (rt == null) {
            expireSession()
            return EzResult.Error(Failure.Auth("Sin refresh_token"))
        }
        val result = oauth.refresh(rt)
        when {
            result is EzResult.Success -> tokenStore.save(result.value)
            result is EzResult.Error && result.failure is Failure.Auth -> expireSession()
        }
        return result.map { }
    }

    private fun expireSession() {
        tokenStore.clear()
        _state.value = SessionState.LoggedOut(expired = true)
    }

    fun idToken(): String? = tokenStore.idToken()

    /** Username de la sesión: claim `sub` del id_token (verificado contra el backend real). */
    fun username(): String? = tokenStore.idToken()?.let { JwtClaims.string(it, "sub") }

    fun logout() {
        tokenStore.clear()
        _state.value = SessionState.LoggedOut()
    }
}
