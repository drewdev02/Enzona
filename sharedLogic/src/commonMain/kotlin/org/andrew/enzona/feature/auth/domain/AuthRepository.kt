package org.andrew.enzona.feature.auth.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.andrew.enzona.core.crypto.Pkce
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.result.map
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore

/** Orquesta el login OAuth y la sesión persistente. */
class AuthRepository(
    private val oauth: OAuthService,
    private val tokenStore: TokenStore,
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emite cuando el refresh_token ya no sirve y hay que volver a iniciar sesión. */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /** Genera el PKCE y la URL de authorize para que la UI nativa la abra. */
    fun startLogin(): Pair<Pkce, String> {
        val pkce = Pkce.generate()
        return pkce to oauth.authorizeUrl(pkce)
    }

    /** Canjea el code capturado del redirect y guarda la sesión. */
    suspend fun completeLogin(code: String, pkce: Pkce): EzResult<Unit> =
        oauth.exchangeCode(code, pkce).also { result ->
            if (result is EzResult.Success) tokenStore.save(result.value)
        }.map { }

    /**
     * Renueva el access_token con el refresh_token guardado.
     * Si el servidor lo rechaza (Failure.Auth) la sesión se da por perdida: se borran los
     * tokens y se emite [sessionExpired]. Un fallo de red NO cierra la sesión.
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
        _sessionExpired.tryEmit(Unit)
    }

    fun isLoggedIn(): Boolean = tokenStore.hasSession()

    fun idToken(): String? = tokenStore.idToken()

    fun logout() = tokenStore.clear()
}
